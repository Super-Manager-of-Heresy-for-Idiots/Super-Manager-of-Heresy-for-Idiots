# Отчёт: архитектура асинхронности и оптимизация под высокую нагрузку

Дата: 2026-06-07
Контекст: Spring Boot 3.4.1, Java 21, PostgreSQL, JPA/Hibernate, STOMP WebSocket.
Цель: подготовить приложение к высокой нагрузке и горизонтальному масштабированию
(несколько подов), с автоматическим использованием выданных поду ресурсов.

---

## 0. Ключевой факт, определивший все решения

В `application.yml` включены **виртуальные потоки** Java 21:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Это значит, что каждый HTTP-запрос Tomcat обрабатывает на виртуальном потоке. При
блокирующем вызове (JDBC, сериализация и т.п.) виртуальный поток **паркуется**, а несущий
платформенный поток освобождается под другую работу. Рантайм может держать тысячи
одновременных блокирующих запросов на горстке несущих потоков.

Из этого вытекают два следствия, на которых построены решения ниже:

1. **Отдельный пул потоков для «асинхронности» больше не нужен и вреден.** Ограниченный
   пул платформенных потоков превращается в искусственный потолок параллелизма.
2. **Настоящая точка backpressure под нагрузкой — пул соединений к БД, а не потоки.**
   Бороться за пропускную способность нужно на уровне HikariCP, а не числа потоков.

---

## 1. Executor контроллеров → виртуальные потоки (`config/AsyncConfig.java`)

### Что было
`controllerTaskExecutor` — `ThreadPoolTaskExecutor` с фиксированными границами
(core 8, max 32, queue 200). При включённых виртуальных потоках это ограничивало
весь трафик 32 потоками: под нагрузкой запросы выстраивались в очередь на 33-м,
хотя рантайм мог бы обслуживать на порядки больше.

### Что стало
```java
@Bean(destroyMethod = "shutdown")
public Executor controllerTaskExecutor() {
    return new DelegatingSecurityContextExecutorService(
            Executors.newVirtualThreadPerTaskExecutor());
}
```

- **Безлимитный**: одна дешёвая виртуальная нить на задачу — параллелизм растёт вместе с
  нагрузкой, а не упирается в размер пула.
- **`DelegatingSecurityContextExecutorService`** (из Spring Security) переносит
  `SecurityContext` с потока запроса на рабочий поток — это заменило самописный
  `TaskDecorator` и гарантирует, что `SecurityContextHolder` остаётся валиден вне потока
  запроса (нужно, в частности, глобальному обработчику исключений).

### Почему контроллеры оставлены на `CompletableFuture`
Сигнатуры `CompletableFuture<...>` сохранены (как было сделано ранее по вашему запросу).
С виртуальными потоками дополнительный «прыжок» на рабочую нить практически бесплатен,
а пул теперь безлимитный, поэтому горлышко устранено. Альтернатива — вернуть контроллеры
к синхронному виду (тоже корректно с виртуальными потоками) — отложена как необязательная:
текущий вариант даёт ту же пропускную способность и не требует переписывать 19 контроллеров.

### Масштабирование по ресурсам пода
Несущий пул виртуальных потоков по умолчанию равен `Runtime.availableProcessors()`,
а это значение **уважает CPU-лимиты контейнера** (cgroups). Значит, каждый под
автоматически масштабирует параллелизм под выданный ему CPU без какой-либо ручной
настройки. Практический предел числа «в полёте» виртуальных нитей — память пода;
память и размер пула БД нужно подбирать совместно (см. §2).

### Сервисы НЕ переведены на `CompletableFuture` — намеренно
Сервисы плотно транзакционны (`@Transactional`, >100 аннотаций). Возврат
`CompletableFuture` из транзакционного метода ломает управление транзакцией: прокси
коммитит/откатывает в момент возврата future, а не по завершении асинхронной работы →
работа с БД вне транзакции, закрытый `EntityManager`, `LazyInitializationException`.
Правильный слой асинхронности — граница контроллера; сервисы остаются синхронными и
транзакционными.

---

## 2. Пул соединений HikariCP (`application.yml`) — реальная точка backpressure

### Что было
Пул не сконфигурирован → дефолт **10 соединений**. При тысячах виртуальных потоков под
нагрузкой все они конкурируют за 10 коннектов; пул и есть настоящий потолок.

### Что стало
Полностью env-конфигурируемый пул с разумными дефолтами:

```yaml
spring:
  datasource:
    hikari:
      pool-name: dndapp-pool
      maximum-pool-size: ${DB_POOL_MAX_SIZE:10}
      minimum-idle: ${DB_POOL_MIN_IDLE:2}
      connection-timeout: ${DB_POOL_CONNECTION_TIMEOUT_MS:10000}
      idle-timeout: ${DB_POOL_IDLE_TIMEOUT_MS:600000}
      max-lifetime: ${DB_POOL_MAX_LIFETIME_MS:1800000}
      leak-detection-threshold: ${DB_POOL_LEAK_DETECTION_MS:0}
```

### Критично для нескольких подов
Postgres имеет общий лимит `max_connections`. Суммарно кластер открывает
`число_подов × maximum-pool-size` соединений. Размер пула на под обязан подчиняться:

```
maximum-pool-size ≤ (max_connections − резерв) / ожидаемое_число_подов
```

Поэтому размер вынесен в переменную окружения `DB_POOL_MAX_SIZE` — каждый деплой/под
задаёт своё значение под ёмкость своей БД. При большом числе подов и общей БД
рекомендуется поставить внешний пулер (**PgBouncer** в режиме transaction pooling),
тогда поды дешевле делят коннекты.

`connection-timeout` (10с) даёт быстрый отказ при исчерпании пула вместо зависания —
это и есть управляемый backpressure.

---

## 3. Кэширование статических справочных данных (`config/CacheConfig.java`)

### Проблема
`ReferenceDataService` (vanilla-методы) на каждый запрос делал несколько обращений к БД
+ JSON-парсинг каждой строки, отдавая по сути **неизменные** системные 5e-данные. На
каждой загрузке мастера создания персонажа — лишняя нагрузка на CPU и БД.

### Решение
Добавлены зависимости `spring-boot-starter-cache` + Caffeine. Локальный (per-pod)
кэш Caffeine с TTL и ограничением размера, аннотации `@Cacheable` на vanilla-методах
(`getVanillaClasses/Races/Backgrounds/Skills/StatTypes/Currencies/Spells`; для spells
ключ по фильтрам `classId/level/school`).

Параметры env-конфигурируемы:
```
app.cache.reference.ttl-minutes (дефолт 60)
app.cache.reference.max-size    (дефолт 1000)
```

### Почему локальный кэш, а не Redis — и как это согласуется с несколькими подами
- Кэшируются **только статические** системные данные, меняющиеся лишь миграцией/деплоем.
  Межподовая рассинхронизация ограничена TTL и не требует общего канала инвалидации.
- Ноль инфраструктуры (без Redis), чистое поведение при рестартах, каждый под греет кэш
  независимо → линейно масштабируется при добавлении подов.
- **Важное ограничение, заложенное намеренно:** данные, зависящие от кампании/homebrew,
  изменяются в рантайме и в этот кэш-менеджер **не помещены**. Если в будущем их кэшировать,
  понадобится либо очень короткий TTL, либо распределённый кэш с явной инвалидацией (Redis).

---

## 4. WebSocket-рассылки → `@Async` (`service/WebSocketEventService.java`)

### Что было
`sendCampaignEvent` / `sendUserEvent` выполнялись синхронно в потоке запроса: сериализация
payload и доставка в брокер удлиняли критический путь ответа.

### Что стало
Методы помечены `@Async` — выполняются на async-исполнителе (виртуальные потоки, см.
`@EnableAsync` в `AsyncConfig`). Это «выстрелил и забыл»: запрос не ждёт доставки.

### Нюанс корректности (порядок коммита)
Рассылка может произойти чуть раньше, чем закоммитится окружающая транзакция (эта гонка
существовала и в синхронном варианте, т.к. send вызывался внутри метода до коммита).
Сейчас это приемлемо: события — уведомления, клиент всё равно перезапрашивает данные по
REST. **Рекомендация на будущее:** для строгого порядка эмитить события через
`@TransactionalEventListener(phase = AFTER_COMMIT)`, чтобы рассылка гарантированно шла
после коммита.

---

## 5. Multi-pod WebSocket: внешний STOMP-брокер relay (СДЕЛАНО)

### Проблема
Встроенный **SimpleBroker** Spring — in-memory, внутри пода. При нескольких подах клиент,
подключённый по STOMP к поду B, **не получал** бы событие, опубликованное на поде A —
у каждого пода своя таблица подписок. Это был главный блокер горизонтального масштабирования.

### Решение
Брокер сделан **переключаемым** через `app.websocket.relay.enabled` (`config/WebSocketConfig.java`):

- `false` (дефолт) — `SimpleBroker`, для одного пода / dev.
- `true` — `enableStompBrokerRelay("/topic", "/queue")` к внешнему **RabbitMQ** (плагин
  `rabbitmq_stomp`). Все поды подключаются к общему брокеру, поэтому сообщение, опубликованное
  на любом поде, доходит до подписчиков на любом другом поде.

Для **адресных** сообщений пользователю (`convertAndSendToUser` → `/user/queue/...`,
используется в `sendUserEvent`) в relay-режиме дополнительно включены:
```java
.setUserDestinationBroadcast("/topic/unresolved-user-destination")
.setUserRegistryBroadcast("/topic/simp-user-registry")
```
Без них сообщение пользователю, чья сессия живёт на другом поде, не маршрутизировалось бы.

### Инфраструктура
В `C:\SuperHerecy\infra\docker-compose.yml` добавлен сервис `rabbitmq`
(`rabbitmq:3.13-management`):
- порт `61613` — STOMP (для relay), `15672` — management UI;
- плагины включены через смонтированный файл `infra/rabbitmq/enabled_plugins`
  (`[rabbitmq_management,rabbitmq_stomp].`);
- healthcheck; том `rabbitmqdata` для персистентности.

Сервис `backend` получил `APP_WEBSOCKET_RELAY_*` переменные (host=`rabbitmq`, port=`61613`,
login/passcode=`dnd`) и `depends_on: rabbitmq (service_healthy)`. На стороне приложения
добавлена зависимость `io.projectreactor.netty:reactor-netty` — TCP-клиент, который
Spring использует для подключения к брокеру.

### Замечания по эксплуатации
- Текущий compose поднимает **один** `backend` (relay уже включён, так что добавление
  реплик backend сразу корректно работает с WebSocket — это и было целью).
- Для production RabbitMQ кластеризуется отдельно; `relay.host` тогда указывает на
  его сервис/LB. Креды вынести из compose в секреты.
- Альтернатива RabbitMQ — Redis Pub/Sub как шина; выбран RabbitMQ из-за нативной
  поддержки STOMP-relay в Spring (минимум кода).

---

## 6. WS отдельным контейнером + REST в два пода (СДЕЛАНО)

### Подход: один образ, две роли
Приложение — монолит, и (важно!) **в коде нет ни одного `@MessageMapping`/`@SubscribeMapping`**:
клиенты только подписываются, все сообщения генерит сервер. То есть WS — чистый fan-out.
Это позволило разделить роли **без выделения отдельного модуля**: тот же jar/образ
(`dnd-app:local`) запускается в двух ролях, отличающихся только переменными окружения.

Управляет ролью флаг `app.websocket.endpoint.enabled` (`config/WebSocketConfig.java`):

- **REST-роль** (`APP_WEBSOCKET_ENDPOINT_ENABLED=false`): эндпоинт `/ws` НЕ регистрируется,
  узел только **публикует** события в relay через `SimpMessagingTemplate`. Масштабируется
  по CPU/БД.
- **WS-роль** (`APP_WEBSOCKET_ENDPOINT_ENABLED=true`): регистрирует `/ws`, терминирует
  клиентские соединения, релеит в RabbitMQ. Масштабируется по числу коннектов.
- Брокер (relay) сконфигурирован в **обеих** ролях, поэтому публикация работает везде.

### Почему WS-узел всё ещё держит JPA/БД
STOMP-интерсептор аутентификации (`WebSocketAuthInterceptor`) на CONNECT/SUBSCRIBE читает
пользователя (`UserRepository`) и членство в кампании (`CampaignMemberRepository`) из БД.
Поэтому полностью «безбазовый» тонкий gateway сейчас невозможен без переноса этих данных в
JWT-claims. Компромисс: WS-узел сохраняет JPA, но запускается с **маленьким пулом**
(`DB_POOL_MAX_SIZE=3`), т.к. он connection-bound, а не query-bound.
**Рекомендация на будущее:** положить роль и membership в JWT → тогда WS-тир станет
полностью stateless относительно БД и масштабируется ещё дешевле.

### Топология в `infra/docker-compose.yml`
Через YAML-anchors (`x-app-image`, `x-app-env`, `x-app-deps`) описаны два сервиса на одном образе:

- **`backend`** — REST-тир, `deploy.replicas: 2` («два пода»), `ENDPOINT_ENABLED=false`,
  пул 10. Убраны `container_name` и проброс порта (иначе реплики конфликтуют); nginx
  достучится по DNS round-robin `backend:8080`.
- **`ws`** — WS-тир, `ENDPOINT_ENABLED=true`, пул 3. По умолчанию 1 реплика.

### Запуск
```bash
cd C:\SuperHerecy\infra
docker compose up -d --build          # backend поднимется в 2 репликах (replicas: 2)
# вручную перекрыть масштаб:
docker compose up -d --scale backend=3 --scale ws=2
```

### Маршрутизация (требует правок в FE-репозитории — см. промпт)
nginx фронта обязан направлять `/api/ → backend:8080`, а `/ws → ws:8080` (с WebSocket
upgrade-заголовками). Файл `nginx.conf` лежит в репозитории фронта, поэтому правка вынесена
в промпт `infra/FRONTEND_CHANGES_PROMPT.md`. Сам FE-код менять не нужно — он уже коннектится
на относительный `/ws`.

### Масштабирование WS-тира (>1 реплики)
SockJS при HTTP-fallback держит сессию из нескольких запросов, которые обязаны попадать в
**один** под → при `--scale ws>1` нужны **sticky sessions** в nginx для `/ws`
(`ip_hash`/sticky cookie) либо отказ от SockJS-fallback в пользу нативного WebSocket.
Межподовую доставку сообщений relay уже обеспечивает; sticky нужен только для целостности
самой SockJS-сессии.

---

## 7. Что осталось за рамками (рекомендации следующего шага)

Не сделано (требует адресной работы по запросам/схеме, отдельный трек, не про async):
- **`ReferenceDataService.getSpells`** фильтрует `availableToClassIdsJson` через
  `String.contains` **в памяти** после выборки всех заклинаний — следует перенести фильтр
  в SQL-запрос.
- Охота на **N+1** в маппинге сущностей в DTO; проверка индексов под частые выборки;
  дефолтные размеры страниц в пагинируемых эндпоинтах.
- При необходимости — параллельное выполнение **независимых** тяжёлых под-запросов внутри
  отдельных методов (через `CompletableFuture` на виртуальных нитях, каждая под-задача в
  своём `@Transactional`-бине). Выигрыш с виртуальными потоками умеренный — точечно.

---

## 8. Сводка изменённых/созданных файлов

| Файл | Изменение |
|------|-----------|
| `config/AsyncConfig.java` | Executor переведён на виртуальные потоки + `DelegatingSecurityContextExecutorService`; `@EnableAsync` |
| `config/CacheConfig.java` | **Новый**: Caffeine cache-manager для vanilla-справочников, env-конфигурируемый TTL/размер |
| `application.yml` | Блок `spring.datasource.hikari`; `app.websocket.endpoint.enabled`; `app.websocket.relay.*` |
| `build.gradle.kts` | Добавлены `spring-boot-starter-cache`, `caffeine`, `reactor-netty` |
| `service/ReferenceDataService.java` | `@Cacheable` на 7 vanilla-методах |
| `service/WebSocketEventService.java` | `@Async` на рассылках |
| `config/WebSocketConfig.java` | Переключаемый брокер (SimpleBroker ↔ STOMP relay) + переключаемый `/ws` эндпоинт (роль REST/WS) |
| `infra/docker-compose.yml` | Anchors; `rabbitmq`; `backend` (REST, replicas:2); новый сервис `ws`; общий образ `dnd-app:local` |
| `infra/rabbitmq/enabled_plugins` | **Новый**: включает `rabbitmq_management`, `rabbitmq_stomp` |
| `infra/FRONTEND_CHANGES_PROMPT.md` | **Новый**: промпт для правок FE (nginx `/ws`, vite dev-proxy) |

Все изменения backend скомпилированы: `BUILD SUCCESSFUL`. `docker compose config` — валиден.

### Чек-лист переменных окружения для прод/масштабирования
```
DB_POOL_MAX_SIZE              # размер пула на под; см. формулу в §2
DB_POOL_MIN_IDLE
DB_POOL_CONNECTION_TIMEOUT_MS
DB_POOL_MAX_LIFETIME_MS
DB_POOL_IDLE_TIMEOUT_MS
DB_POOL_LEAK_DETECTION_MS     # >0 в non-prod для отлова утечек коннектов
app.cache.reference.ttl-minutes
app.cache.reference.max-size
APP_WEBSOCKET_RELAY_ENABLED        # true для multi-pod (внешний STOMP relay)
APP_WEBSOCKET_RELAY_HOST           # хост RabbitMQ (в compose: rabbitmq)
APP_WEBSOCKET_RELAY_PORT           # STOMP-порт (61613)
APP_WEBSOCKET_RELAY_LOGIN
APP_WEBSOCKET_RELAY_PASSCODE
APP_WEBSOCKET_RELAY_VIRTUAL_HOST   # дефолт "/"
```
