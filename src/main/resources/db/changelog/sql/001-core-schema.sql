--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ability_score; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ability_score (
    ability_score_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text NOT NULL,
    homebrew_id uuid
);


--
-- Name: armor_stat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.armor_stat (
    equipment_item_id uuid NOT NULL,
    base_ac integer,
    dex_bonus_allowed boolean DEFAULT false NOT NULL,
    max_dex_bonus integer,
    strength_required integer,
    stealth_disadvantage boolean DEFAULT false NOT NULL,
    armor_class_raw text
);


--
-- Name: background; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background (
    background_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mod_id uuid,
    source_id uuid,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    feat_id uuid,
    description text,
    url text,
    homebrew_id uuid
);


--
-- Name: background_ability_option; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background_ability_option (
    background_id uuid NOT NULL,
    ability_score_id uuid NOT NULL
);


--
-- Name: background_equipment_choice_group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background_equipment_choice_group (
    background_equipment_choice_group_id uuid DEFAULT gen_random_uuid() NOT NULL,
    background_id uuid NOT NULL,
    group_slug text NOT NULL,
    choose_count integer,
    raw_text text
);


--
-- Name: background_equipment_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background_equipment_entry (
    background_equipment_entry_id uuid DEFAULT gen_random_uuid() NOT NULL,
    background_equipment_option_id uuid NOT NULL,
    entry_type text NOT NULL,
    equipment_item_id uuid,
    money_value_id uuid,
    quantity numeric(38,2) DEFAULT 1 NOT NULL,
    quantity_unit_raw text,
    variant_note text,
    choice_ref text,
    raw_text text
);


--
-- Name: background_equipment_option; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background_equipment_option (
    background_equipment_option_id uuid DEFAULT gen_random_uuid() NOT NULL,
    background_equipment_choice_group_id uuid NOT NULL,
    option_code text NOT NULL,
    sort_order integer NOT NULL,
    raw_text text
);


--
-- Name: background_feat_option; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background_feat_option (
    background_feat_option_id uuid DEFAULT gen_random_uuid() NOT NULL,
    background_id uuid NOT NULL,
    feat_id uuid,
    feat_category_id uuid,
    choose_count integer DEFAULT 1 NOT NULL,
    selected_option_raw text,
    recommended_feat_id uuid,
    raw_text text
);


--
-- Name: background_language_proficiency; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background_language_proficiency (
    background_language_proficiency_id uuid DEFAULT gen_random_uuid() NOT NULL,
    background_id uuid,
    language_slug text,
    choose_count integer,
    raw_text text
);


--
-- Name: background_skill_proficiency; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background_skill_proficiency (
    background_id uuid NOT NULL,
    skill_id uuid NOT NULL
);


--
-- Name: background_tool_proficiency; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.background_tool_proficiency (
    background_tool_proficiency_id uuid DEFAULT gen_random_uuid() NOT NULL,
    background_id uuid,
    equipment_item_id uuid,
    choose_count integer,
    choice_group_slug text,
    raw_text text
);


--
-- Name: battle_combatants; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.battle_combatants (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    battle_id uuid NOT NULL,
    combatant_type character varying(12) NOT NULL,
    monster_id uuid,
    character_id uuid,
    display_name character varying(140) NOT NULL,
    instance_index integer,
    initiative integer,
    initiative_roll integer,
    dex_tiebreak integer,
    turn_order integer,
    current_hp integer,
    max_hp integer,
    added_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: battles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.battles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    campaign_id uuid NOT NULL,
    name character varying(120),
    status character varying(20) DEFAULT 'ASSEMBLING'::character varying NOT NULL,
    override_xp integer,
    round_number integer DEFAULT 1 NOT NULL,
    current_turn_index integer DEFAULT 0 NOT NULL,
    created_by uuid NOT NULL,
    started_at timestamp with time zone,
    ended_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: blueprint_homebrew; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.blueprint_homebrew (
    blueprint_id uuid NOT NULL,
    package_id uuid NOT NULL,
    pinned_version integer
);


--
-- Name: blueprint_locations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.blueprint_locations (
    id uuid NOT NULL,
    blueprint_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    is_visible_to_players boolean DEFAULT false NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: blueprint_npc_spells; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.blueprint_npc_spells (
    npc_id uuid NOT NULL,
    spell_id uuid NOT NULL
);


--
-- Name: blueprint_npcs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.blueprint_npcs (
    id uuid NOT NULL,
    blueprint_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    is_visible_to_players boolean DEFAULT false NOT NULL,
    public_description text,
    private_description text,
    created_by uuid NOT NULL,
    source_type character varying(20),
    race_id uuid,
    class_id uuid,
    level integer,
    abilities text,
    source_monster_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: blueprint_quest_rewards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.blueprint_quest_rewards (
    id uuid NOT NULL,
    quest_id uuid NOT NULL,
    item_template_id uuid,
    quantity integer DEFAULT 1,
    currency_type_id uuid,
    currency_amount numeric(15,2),
    xp_amount integer
);


--
-- Name: blueprint_quests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.blueprint_quests (
    id uuid NOT NULL,
    blueprint_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    is_visible_to_players boolean DEFAULT false NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: buffs_debuffs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.buffs_debuffs (
    id uuid NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    effect_type character varying(30) NOT NULL,
    target_stat_id uuid,
    modifier_value integer,
    duration_rounds integer,
    is_buff boolean NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    homebrew_id uuid,
    name_engloc text,
    name_rusloc text,
    description_engloc text,
    description_rusloc text,
    deprecated boolean DEFAULT false NOT NULL
);


--
-- Name: campaign_blueprints; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campaign_blueprints (
    id uuid NOT NULL,
    author_id uuid NOT NULL,
    parent_id uuid,
    origin_version integer,
    universe_id uuid NOT NULL,
    title character varying(120) NOT NULL,
    lore_description text,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    version integer DEFAULT 1 NOT NULL,
    allow_forks boolean DEFAULT true NOT NULL,
    download_count integer DEFAULT 0 NOT NULL,
    cover_url text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    published_at timestamp with time zone,
    deleted_at timestamp with time zone,
    deleted_by uuid
);


--
-- Name: campaign_homebrew; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campaign_homebrew (
    campaign_id uuid NOT NULL,
    package_id uuid NOT NULL,
    pinned_version integer
);


--
-- Name: campaign_locations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campaign_locations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    campaign_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    is_visible_to_players boolean DEFAULT false NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: campaign_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campaign_members (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    campaign_id uuid NOT NULL,
    role_in_campaign character varying(10) NOT NULL,
    is_creator boolean DEFAULT false NOT NULL,
    joined_at timestamp with time zone DEFAULT now() NOT NULL,
    kicked boolean DEFAULT false NOT NULL
);


--
-- Name: campaign_npc_spells; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campaign_npc_spells (
    npc_id uuid NOT NULL,
    spell_id uuid NOT NULL
);


--
-- Name: campaign_npcs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campaign_npcs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    campaign_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    is_visible_to_players boolean DEFAULT false NOT NULL,
    public_description text,
    private_description text,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    source_type character varying(20),
    race_id uuid,
    class_id uuid,
    level integer,
    abilities text,
    source_monster_id uuid
);


--
-- Name: campaign_quests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campaign_quests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    campaign_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    is_visible_to_players boolean DEFAULT false NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: campaigns; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campaigns (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(120) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    invite_code character varying(8) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: character_active_effects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_active_effects (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    character_id uuid NOT NULL,
    buff_debuff_id uuid NOT NULL,
    applied_by uuid NOT NULL,
    remaining_rounds integer,
    applied_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: character_class; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_class (
    class_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mod_id uuid,
    source_id uuid,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    subtitle text,
    url text,
    homebrew_id uuid,
    hit_die integer,
    is_spellcaster boolean DEFAULT false NOT NULL,
    has_cantrips boolean DEFAULT false NOT NULL,
    is_half_caster boolean DEFAULT false NOT NULL,
    spellcasting_ability_id uuid,
    skill_choice_count integer DEFAULT 0 NOT NULL,
    skill_choice_any boolean DEFAULT false NOT NULL,
    armor_proficiency_text text,
    weapon_proficiency_text text,
    tool_proficiency_text text
);


--
-- Name: character_class_levels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_class_levels (
    character_id uuid NOT NULL,
    class_id uuid NOT NULL,
    class_level integer DEFAULT 1 NOT NULL,
    CONSTRAINT chk_ccl_class_level CHECK (((class_level >= 1) AND (class_level <= 20)))
);


--
-- Name: character_known_spells; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_known_spells (
    id uuid NOT NULL,
    character_id uuid NOT NULL,
    spell_id uuid NOT NULL
);


--
-- Name: character_resources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_resources (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    character_id uuid NOT NULL,
    resource_type_id uuid NOT NULL,
    current_value integer DEFAULT 0 NOT NULL
);


--
-- Name: character_reward_ability_score_selection; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_reward_ability_score_selection (
    character_reward_selection_id uuid NOT NULL,
    reward_grant_id uuid NOT NULL,
    ability_score_id uuid NOT NULL,
    bonus_amount integer NOT NULL
);


--
-- Name: character_reward_selection; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_reward_selection (
    character_reward_selection_id uuid DEFAULT gen_random_uuid() NOT NULL,
    character_id uuid NOT NULL,
    reward_group_id uuid NOT NULL,
    reward_option_id uuid NOT NULL,
    selected_at timestamp with time zone DEFAULT now() NOT NULL,
    note_text text
);


--
-- Name: character_reward_skill_selection; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_reward_skill_selection (
    character_reward_selection_id uuid NOT NULL,
    reward_grant_id uuid NOT NULL,
    skill_id uuid NOT NULL
);


--
-- Name: character_reward_spell_selection; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_reward_spell_selection (
    character_reward_selection_id uuid NOT NULL,
    reward_grant_id uuid NOT NULL,
    spell_id uuid NOT NULL
);


--
-- Name: character_size; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_size (
    character_size_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    homebrew_id uuid
);


--
-- Name: character_skill_proficiencies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_skill_proficiencies (
    id uuid NOT NULL,
    character_id uuid NOT NULL,
    skill_id uuid NOT NULL,
    source character varying(20) NOT NULL,
    proficiency_level text DEFAULT 'PROFICIENT'::text NOT NULL,
    CONSTRAINT chk_character_skill_proficiency_level CHECK ((proficiency_level = ANY (ARRAY['PROFICIENT'::text, 'EXPERTISE'::text])))
);


--
-- Name: character_spell_slot_usage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_spell_slot_usage (
    character_id uuid NOT NULL,
    spell_level integer NOT NULL,
    expended_count integer DEFAULT 0 NOT NULL,
    CONSTRAINT character_spell_slot_usage_expended_count_check CHECK ((expended_count >= 0)),
    CONSTRAINT character_spell_slot_usage_spell_level_check CHECK (((spell_level >= 1) AND (spell_level <= 9)))
);


--
-- Name: character_stats; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_stats (
    id uuid NOT NULL,
    character_id uuid NOT NULL,
    stat_type_id uuid NOT NULL,
    value integer DEFAULT 10 NOT NULL,
    deprecated boolean DEFAULT false NOT NULL
);


--
-- Name: character_wallets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.character_wallets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    character_id uuid NOT NULL,
    currency_type_id uuid NOT NULL,
    amount numeric(15,2) DEFAULT 0 NOT NULL
);


--
-- Name: characters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.characters (
    id uuid NOT NULL,
    name character varying(100) NOT NULL,
    total_level integer DEFAULT 1 NOT NULL,
    race_id uuid NOT NULL,
    owner_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    experience bigint DEFAULT 0 NOT NULL,
    campaign_id uuid,
    status character varying(10) DEFAULT 'ACTIVE'::character varying NOT NULL,
    current_hp integer,
    max_hp integer,
    selected_lineage_id uuid,
    race_snapshot_json text,
    alignment character varying(40),
    background_id uuid,
    avatar_url text,
    armor_class integer,
    speed integer,
    inspiration boolean DEFAULT false,
    hit_dice_type character varying(10),
    hit_dice_total character varying(20),
    death_save_successes integer DEFAULT 0,
    death_save_failures integer DEFAULT 0,
    saving_throw_proficiency_stat_ids_json text,
    biography_json text,
    features text,
    attacks_json text,
    score_method character varying(20),
    temp_hp integer DEFAULT 0 NOT NULL,
    player_name character varying(100),
    proficiencies text,
    equipment text,
    blueprint_id uuid,
    CONSTRAINT chk_characters_death_save_failures CHECK (((death_save_failures >= 0) AND (death_save_failures <= 3))),
    CONSTRAINT chk_characters_death_save_successes CHECK (((death_save_successes >= 0) AND (death_save_successes <= 3))),
    CONSTRAINT chk_characters_total_level CHECK (((total_level >= 1) AND (total_level <= 20)))
);


--
-- Name: class_authoring_idempotency; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_authoring_idempotency (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    scope text NOT NULL,
    idem_key text NOT NULL,
    request_hash text NOT NULL,
    result_class_id uuid NOT NULL,
    package_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: class_feature; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_feature (
    class_feature_id uuid DEFAULT gen_random_uuid() NOT NULL,
    class_id uuid NOT NULL,
    subclass_id uuid,
    slug text NOT NULL,
    level integer,
    sort_order integer NOT NULL,
    title text NOT NULL,
    description text
);


--
-- Name: class_level_reward_grant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant (
    reward_grant_id uuid DEFAULT gen_random_uuid() NOT NULL,
    reward_group_id uuid,
    reward_option_id uuid,
    grant_type text NOT NULL,
    label_ru text,
    label_en text,
    description text,
    sort_order integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_class_reward_grant_owner CHECK ((((reward_group_id IS NOT NULL) AND (reward_option_id IS NULL)) OR ((reward_group_id IS NULL) AND (reward_option_id IS NOT NULL))))
);


--
-- Name: class_level_reward_grant_ability_option; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_ability_option (
    reward_grant_id uuid NOT NULL,
    ability_score_id uuid NOT NULL
);


--
-- Name: class_level_reward_grant_ability_score; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_ability_score (
    reward_grant_id uuid NOT NULL,
    ability_score_id uuid,
    choose_count integer DEFAULT 1 NOT NULL,
    bonus_per_choice integer DEFAULT 1 NOT NULL,
    total_bonus integer,
    max_per_ability integer,
    max_score integer,
    raw_filter_text text,
    CONSTRAINT chk_class_reward_ability_bonus CHECK ((bonus_per_choice <> 0)),
    CONSTRAINT chk_class_reward_ability_choice_count CHECK ((choose_count > 0))
);


--
-- Name: class_level_reward_grant_custom_text; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_custom_text (
    reward_grant_id uuid NOT NULL,
    title_ru text,
    title_en text,
    body text NOT NULL,
    user_editable boolean DEFAULT true NOT NULL
);


--
-- Name: class_level_reward_grant_feat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_feat (
    reward_grant_id uuid NOT NULL,
    feat_id uuid NOT NULL
);


--
-- Name: class_level_reward_grant_feature; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_feature (
    reward_grant_id uuid NOT NULL,
    class_feature_id uuid NOT NULL
);


--
-- Name: class_level_reward_grant_numeric_modifier; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_numeric_modifier (
    reward_grant_id uuid NOT NULL,
    modifier_key text NOT NULL,
    target_kind text NOT NULL,
    target_label_ru text,
    target_label_en text,
    amount numeric(12,3),
    dice_formula_id uuid,
    unit_text text,
    duration_text text,
    stacking_rule text,
    notes text
);


--
-- Name: class_level_reward_grant_skill_option; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_skill_option (
    reward_grant_id uuid NOT NULL,
    skill_id uuid NOT NULL
);


--
-- Name: class_level_reward_grant_skill_proficiency; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_skill_proficiency (
    reward_grant_id uuid NOT NULL,
    skill_id uuid,
    choose_count integer DEFAULT 1 NOT NULL,
    any_skill boolean DEFAULT false NOT NULL,
    raw_filter_text text,
    grants_expertise boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_class_reward_skill_choice_count CHECK ((choose_count > 0))
);


--
-- Name: class_level_reward_grant_spell; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_spell (
    reward_grant_id uuid NOT NULL,
    spell_id uuid,
    spell_level integer,
    school_id uuid,
    choose_count integer DEFAULT 1 NOT NULL,
    raw_filter_text text,
    CONSTRAINT chk_class_reward_spell_choice_count CHECK ((choose_count > 0))
);


--
-- Name: class_level_reward_grant_subclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_grant_subclass (
    reward_grant_id uuid NOT NULL,
    subclass_id uuid NOT NULL
);


--
-- Name: class_level_reward_group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_group (
    reward_group_id uuid DEFAULT gen_random_uuid() NOT NULL,
    class_id uuid NOT NULL,
    class_feature_id uuid,
    class_level integer NOT NULL,
    group_kind text NOT NULL,
    prompt_ru text,
    prompt_en text,
    description text,
    choose_min integer DEFAULT 0 NOT NULL,
    choose_max integer DEFAULT 1 NOT NULL,
    repeatable boolean DEFAULT false NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_class_reward_group_choice_count CHECK (((choose_min >= 0) AND (choose_max >= choose_min))),
    CONSTRAINT class_level_reward_group_class_level_check CHECK (((class_level >= 1) AND (class_level <= 20))),
    CONSTRAINT class_level_reward_group_group_kind_check CHECK ((group_kind = ANY (ARRAY['AUTO'::text, 'CHOICE'::text])))
);


--
-- Name: class_level_reward_option; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_level_reward_option (
    reward_option_id uuid DEFAULT gen_random_uuid() NOT NULL,
    reward_group_id uuid NOT NULL,
    option_key text,
    label_ru text NOT NULL,
    label_en text,
    description text,
    is_recommended boolean DEFAULT false NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);


--
-- Name: class_primary_ability; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_primary_ability (
    class_id uuid NOT NULL,
    ability_score_id uuid NOT NULL
);


--
-- Name: class_progression_column; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_progression_column (
    class_progression_column_id uuid DEFAULT gen_random_uuid() NOT NULL,
    class_id uuid NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    sort_order integer NOT NULL
);


--
-- Name: class_progression_value; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_progression_value (
    class_id uuid NOT NULL,
    class_level integer NOT NULL,
    class_progression_column_id uuid NOT NULL,
    value_raw text,
    value_numeric integer,
    CONSTRAINT class_progression_value_class_level_check CHECK (((class_level >= 1) AND (class_level <= 20)))
);


--
-- Name: class_saving_throw; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_saving_throw (
    class_id uuid NOT NULL,
    ability_score_id uuid NOT NULL
);


--
-- Name: class_skill_option; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_skill_option (
    class_id uuid NOT NULL,
    skill_id uuid NOT NULL
);


--
-- Name: creature_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.creature_type (
    creature_type_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text
);


--
-- Name: currency; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.currency (
    currency_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    abbr_ru text NOT NULL,
    abbr_en text,
    copper_value numeric(12,3) NOT NULL,
    homebrew_id uuid
);


--
-- Name: custom_resource_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.custom_resource_types (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    max_value integer,
    homebrew_id uuid,
    class_bound_id uuid,
    name_engloc text,
    name_rusloc text,
    description_engloc text,
    description_rusloc text
);


--
-- Name: damage_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.damage_type (
    damage_type_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    homebrew_id uuid
);


--
-- Name: damage_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.damage_types (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(60) NOT NULL,
    name_engloc text,
    name_rusloc text NOT NULL,
    homebrew_id uuid,
    is_unique boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: dice_formula; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dice_formula (
    dice_formula_id uuid DEFAULT gen_random_uuid() NOT NULL,
    dice_count integer,
    die_size integer,
    bonus integer,
    raw_text text
);


--
-- Name: enchantment_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enchantment_types (
    id uuid NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    damage_dice character varying(10),
    damage_bonus integer DEFAULT 0 NOT NULL,
    buff_debuff_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    homebrew_id uuid,
    name_engloc text,
    name_rusloc text,
    description_engloc text,
    description_rusloc text,
    damage_type_id uuid
);


--
-- Name: equipment_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.equipment_category (
    equipment_category_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    homebrew_id uuid
);


--
-- Name: equipment_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.equipment_item (
    equipment_item_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mod_id uuid,
    source_id uuid,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    category_id uuid,
    kind text NOT NULL,
    cost_money_value_id uuid,
    weight_lb numeric(12,4),
    properties_text text,
    url text,
    homebrew_id uuid
);


--
-- Name: equipment_slots; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.equipment_slots (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(60) NOT NULL,
    name_engloc text,
    name_rusloc text NOT NULL,
    homebrew_id uuid,
    is_unique boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: feat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feat (
    feat_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mod_id uuid,
    source_id uuid,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    category_id uuid,
    repeatable boolean DEFAULT false NOT NULL,
    description text,
    url text,
    homebrew_id uuid
);


--
-- Name: feat_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feat_category (
    feat_category_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text
);


--
-- Name: feat_prerequisite; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feat_prerequisite (
    feat_prerequisite_id uuid DEFAULT gen_random_uuid() NOT NULL,
    feat_id uuid NOT NULL,
    prerequisite_type text NOT NULL,
    level_required integer,
    ability_score_id uuid,
    minimum_score integer,
    group_key text,
    raw_text text
);


--
-- Name: feat_section; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feat_section (
    feat_section_id uuid DEFAULT gen_random_uuid() NOT NULL,
    feat_id uuid NOT NULL,
    sort_order integer NOT NULL,
    title text,
    body text
);


--
-- Name: gm_homebrew_library; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gm_homebrew_library (
    gm_user_id uuid NOT NULL,
    package_id uuid NOT NULL,
    added_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: gm_session_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gm_session_notes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    campaign_id uuid NOT NULL,
    author_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: homebrew_content_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.homebrew_content_items (
    id uuid NOT NULL,
    package_id uuid NOT NULL,
    content_type character varying(30) NOT NULL,
    content_id uuid NOT NULL
);


--
-- Name: homebrew_content_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.homebrew_content_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    package_id uuid NOT NULL,
    version integer NOT NULL,
    content_type character varying(30) NOT NULL,
    content_id uuid NOT NULL,
    change_type character varying(20) NOT NULL,
    changed_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: homebrew_package_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.homebrew_package_tags (
    package_id uuid NOT NULL,
    tag_id uuid NOT NULL
);


--
-- Name: homebrew_packages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.homebrew_packages (
    id uuid NOT NULL,
    author_id uuid NOT NULL,
    title character varying(120) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    version integer DEFAULT 1 NOT NULL,
    download_count integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    published_at timestamp with time zone,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    parent_id uuid,
    is_removable boolean DEFAULT true NOT NULL
);


--
-- Name: homebrew_ratings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.homebrew_ratings (
    user_id uuid NOT NULL,
    package_id uuid NOT NULL,
    rating integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_rating_value CHECK ((rating = ANY (ARRAY['-1'::integer, 1])))
);


--
-- Name: homebrew_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.homebrew_tags (
    id uuid NOT NULL,
    name character varying(50) NOT NULL
);


--
-- Name: import_warning; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.import_warning (
    import_warning_id uuid DEFAULT gen_random_uuid() NOT NULL,
    source_slug text,
    entity_kind text,
    entity_slug text,
    warning_code text NOT NULL,
    message text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: item_enchantments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.item_enchantments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    item_instance_id uuid NOT NULL,
    enchantment_type_id uuid NOT NULL,
    applied_at timestamp with time zone DEFAULT now() NOT NULL,
    notes character varying(255)
);


--
-- Name: item_instances; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.item_instances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    template_id uuid,
    equipment_item_id uuid,
    magic_item_id uuid,
    owner_character_id uuid,
    shared_storage_id uuid,
    custom_name character varying(100),
    quantity integer DEFAULT 1 NOT NULL,
    is_unique boolean DEFAULT false NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    slot_id uuid,
    CONSTRAINT chk_iteminst_one_ref CHECK ((((
        CASE WHEN (template_id IS NOT NULL) THEN 1 ELSE 0 END
      + CASE WHEN (equipment_item_id IS NOT NULL) THEN 1 ELSE 0 END
      + CASE WHEN (magic_item_id IS NOT NULL) THEN 1 ELSE 0 END)) = 1))
);


--
-- Name: item_rarities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.item_rarities (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(60) NOT NULL,
    name_engloc text,
    name_rusloc text NOT NULL,
    homebrew_id uuid,
    is_unique boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: item_template_buffs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.item_template_buffs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    template_id uuid NOT NULL,
    buff_debuff_id uuid NOT NULL,
    homebrew_id uuid
);


--
-- Name: item_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.item_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    item_type_id uuid,
    damage_dice character varying(10),
    damage_bonus integer DEFAULT 0,
    is_stackable boolean DEFAULT false NOT NULL,
    skill_id uuid,
    skill_activation character varying(10),
    homebrew_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    name_engloc text,
    name_rusloc text,
    description_engloc text,
    description_rusloc text,
    rarity_id uuid,
    damage_type_id uuid
);


--
-- Name: item_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.item_types (
    id uuid NOT NULL,
    name character varying(50) NOT NULL,
    description text,
    homebrew_id uuid,
    damage_dice character varying(10),
    damage_bonus integer DEFAULT 0 NOT NULL,
    skill_id uuid,
    skill_activation character varying(10),
    properties text,
    base_ac integer,
    max_dex_bonus integer,
    str_requirement integer,
    stealth_disadvantage boolean,
    name_engloc text,
    name_rusloc text,
    description_engloc text,
    description_rusloc text,
    properties_engloc text,
    properties_rusloc text,
    slot_id uuid NOT NULL,
    damage_type_id uuid
);


--
-- Name: magic_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.magic_item (
    magic_item_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mod_id uuid,
    source_id uuid,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    magic_item_type_id uuid,
    type_restriction_raw text,
    rarity_id uuid,
    variable_rarity boolean DEFAULT false NOT NULL,
    attunement_required boolean DEFAULT false NOT NULL,
    attunement_requirement text,
    cost_money_value_id uuid,
    description text,
    embedded_tables_detected boolean DEFAULT false NOT NULL,
    url text,
    homebrew_id uuid
);


--
-- Name: magic_item_allowed_equipment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.magic_item_allowed_equipment (
    magic_item_id uuid NOT NULL,
    equipment_item_id uuid NOT NULL,
    raw_text text
);


--
-- Name: magic_item_rarity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.magic_item_rarity (
    magic_item_rarity_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    sort_order integer,
    homebrew_id uuid
);


--
-- Name: magic_item_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.magic_item_type (
    magic_item_type_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    homebrew_id uuid
);


--
-- Name: mod_package; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mod_package (
    mod_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name text NOT NULL,
    is_core boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: money_value; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.money_value (
    money_value_id uuid DEFAULT gen_random_uuid() NOT NULL,
    amount numeric(12,3),
    currency_id uuid,
    copper_value numeric(14,3),
    raw_text text
);


--
-- Name: npc_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.npc_notes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    npc_id uuid NOT NULL,
    author_id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: proficiency_skills; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.proficiency_skills (
    id uuid NOT NULL,
    name character varying(60) NOT NULL,
    governing_stat_id uuid NOT NULL,
    name_engloc text,
    name_rusloc text,
    homebrew_id uuid,
    deprecated boolean DEFAULT false NOT NULL
);


--
-- Name: quest_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quest_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    quest_id uuid NOT NULL,
    item_template_id uuid NOT NULL
);


--
-- Name: quest_locations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quest_locations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    quest_id uuid NOT NULL,
    location_id uuid NOT NULL
);


--
-- Name: quest_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quest_notes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    quest_id uuid NOT NULL,
    author_id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: quest_npcs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quest_npcs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    quest_id uuid NOT NULL,
    npc_id uuid NOT NULL
);


--
-- Name: quest_rewards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quest_rewards (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    quest_id uuid NOT NULL,
    item_template_id uuid,
    quantity integer DEFAULT 1,
    currency_type_id uuid,
    currency_amount numeric(15,2),
    xp_amount integer
);


--
-- Name: random_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.random_table (
    random_table_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mod_id uuid,
    source_id uuid,
    slug text NOT NULL,
    name_ru text NOT NULL,
    dice text,
    url text
);


--
-- Name: random_table_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.random_table_entry (
    random_table_entry_id uuid DEFAULT gen_random_uuid() NOT NULL,
    random_table_id uuid NOT NULL,
    range_start integer,
    range_end integer,
    display_range text NOT NULL,
    result_text text NOT NULL,
    linked_equipment_item_id uuid,
    linked_magic_item_id uuid
);


--
-- Name: shared_storage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shared_storage (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    campaign_id uuid NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: skill; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.skill (
    skill_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    ability_score_id uuid,
    homebrew_id uuid
);


--
-- Name: skill_effects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.skill_effects (
    id uuid NOT NULL,
    skill_id uuid NOT NULL,
    buff_debuff_id uuid NOT NULL,
    effect_role character varying(10) NOT NULL,
    chance_percent integer DEFAULT 100 NOT NULL,
    homebrew_id uuid
);


--
-- Name: skills; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.skills (
    id uuid NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    skill_type character varying(50),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    homebrew_id uuid,
    damage_dice character varying(10),
    damage_bonus integer DEFAULT 0 NOT NULL,
    scaling_json text,
    name_engloc text,
    name_rusloc text,
    description_engloc text,
    description_rusloc text,
    damage_type_id uuid
);


--
-- Name: source_book; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.source_book (
    source_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name text NOT NULL,
    url text,
    license_note text
);


--
-- Name: species; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.species (
    species_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mod_id uuid,
    source_id uuid,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    creature_type_id uuid,
    description text,
    url text,
    homebrew_id uuid
);


--
-- Name: species_size_option; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.species_size_option (
    species_id uuid NOT NULL,
    character_size_id uuid NOT NULL
);


--
-- Name: species_speed; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.species_speed (
    species_speed_id uuid DEFAULT gen_random_uuid() NOT NULL,
    species_id uuid,
    speed_type_slug text NOT NULL,
    amount_ft integer,
    raw_text text
);


--
-- Name: species_trait; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.species_trait (
    species_trait_id uuid DEFAULT gen_random_uuid() NOT NULL,
    species_id uuid,
    slug text NOT NULL,
    sort_order integer NOT NULL,
    name text NOT NULL,
    description text
);


--
-- Name: species_trait_effect; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.species_trait_effect (
    species_trait_effect_id uuid DEFAULT gen_random_uuid() NOT NULL,
    species_trait_id uuid,
    effect_type text NOT NULL,
    damage_type_id uuid,
    spell_id uuid,
    range_ft integer
);


--
-- Name: spell; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.spell (
    spell_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mod_id uuid,
    source_id uuid,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    level integer NOT NULL,
    school_id uuid,
    casting_time_raw text,
    casting_action_slug text,
    is_ritual boolean DEFAULT false NOT NULL,
    range_type text,
    range_distance integer,
    range_unit text,
    duration_raw text,
    duration_type text,
    duration_amount integer,
    duration_unit text,
    concentration boolean DEFAULT false NOT NULL,
    description text,
    higher_levels text,
    url text,
    homebrew_id uuid,
    CONSTRAINT spell_level_check CHECK (((level >= 0) AND (level <= 9)))
);


--
-- Name: spell_class; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.spell_class (
    spell_id uuid NOT NULL,
    class_id uuid NOT NULL
);


--
-- Name: spell_component; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.spell_component (
    spell_id uuid NOT NULL,
    component_slug text NOT NULL,
    material_text text,
    consumed boolean,
    cost_money_value_id uuid
);


--
-- Name: spell_school; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.spell_school (
    spell_school_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text
);


--
-- Name: spell_scroll_crafting_rule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.spell_scroll_crafting_rule (
    spell_scroll_crafting_rule_id uuid DEFAULT gen_random_uuid() NOT NULL,
    spell_level integer NOT NULL,
    time_days integer NOT NULL,
    cost_money_value_id uuid,
    CONSTRAINT spell_scroll_crafting_rule_spell_level_check CHECK (((spell_level >= 0) AND (spell_level <= 9)))
);


--
-- Name: spell_subclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.spell_subclass (
    spell_id uuid NOT NULL,
    subclass_id uuid NOT NULL,
    raw_text text
);


--
-- Name: subclass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subclass (
    subclass_id uuid DEFAULT gen_random_uuid() NOT NULL,
    class_id uuid NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    is_empty_placeholder boolean DEFAULT false NOT NULL,
    homebrew_id uuid
);


--
-- Name: table_sizes_gb; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.table_sizes_gb AS
 SELECT schemaname AS schema_name,
    relname AS table_name,
    round(((((pg_total_relation_size((relid)::regclass))::numeric / 1024.0) / 1024.0) / 1024.0), 3) AS total_size_gb,
    round(((((pg_relation_size((relid)::regclass))::numeric / 1024.0) / 1024.0) / 1024.0), 3) AS table_size_gb,
    round(((((pg_indexes_size((relid)::regclass))::numeric / 1024.0) / 1024.0) / 1024.0), 3) AS indexes_size_gb,
    pg_size_pretty(pg_total_relation_size((relid)::regclass)) AS total_size_pretty
   FROM pg_statio_user_tables;


--
-- Name: universes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.universes (
    id uuid NOT NULL,
    slug character varying(60) NOT NULL,
    name character varying(120) NOT NULL,
    description text,
    created_by uuid,
    is_system boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    username character varying(30) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(20) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: wallet_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wallet_transactions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    character_id uuid NOT NULL,
    currency_type_id uuid NOT NULL,
    delta numeric(15,2) NOT NULL,
    balance_after numeric(15,2) NOT NULL,
    reason text,
    performed_by character varying(100),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: weapon_item_property; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.weapon_item_property (
    equipment_item_id uuid NOT NULL,
    weapon_property_id uuid NOT NULL,
    normal_range_ft integer,
    long_range_ft integer,
    versatile_dice_formula_id uuid,
    ammunition_equipment_item_id uuid,
    raw_text text
);


--
-- Name: weapon_mastery; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.weapon_mastery (
    weapon_mastery_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text,
    description text
);


--
-- Name: weapon_property; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.weapon_property (
    weapon_property_id uuid DEFAULT gen_random_uuid() NOT NULL,
    slug text NOT NULL,
    name_ru text NOT NULL,
    name_en text
);


--
-- Name: weapon_stat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.weapon_stat (
    equipment_item_id uuid NOT NULL,
    damage_dice_formula_id uuid,
    damage_type_id uuid,
    flat_damage integer,
    mastery_id uuid
);


--
-- Name: ability_score ability_score_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ability_score
    ADD CONSTRAINT ability_score_pkey PRIMARY KEY (ability_score_id);


--
-- Name: armor_stat armor_stat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.armor_stat
    ADD CONSTRAINT armor_stat_pkey PRIMARY KEY (equipment_item_id);


--
-- Name: background_ability_option background_ability_option_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_ability_option
    ADD CONSTRAINT background_ability_option_pkey PRIMARY KEY (background_id, ability_score_id);


--
-- Name: background_equipment_choice_group background_equipment_choice_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_equipment_choice_group
    ADD CONSTRAINT background_equipment_choice_group_pkey PRIMARY KEY (background_equipment_choice_group_id);


--
-- Name: background_equipment_entry background_equipment_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_equipment_entry
    ADD CONSTRAINT background_equipment_entry_pkey PRIMARY KEY (background_equipment_entry_id);


--
-- Name: background_equipment_option background_equipment_option_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_equipment_option
    ADD CONSTRAINT background_equipment_option_pkey PRIMARY KEY (background_equipment_option_id);


--
-- Name: background_feat_option background_feat_option_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_feat_option
    ADD CONSTRAINT background_feat_option_pkey PRIMARY KEY (background_feat_option_id);


--
-- Name: background_language_proficiency background_language_proficiency_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_language_proficiency
    ADD CONSTRAINT background_language_proficiency_pkey PRIMARY KEY (background_language_proficiency_id);


--
-- Name: background background_mod_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background
    ADD CONSTRAINT background_mod_id_slug_key UNIQUE (mod_id, slug);


--
-- Name: background background_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background
    ADD CONSTRAINT background_pkey PRIMARY KEY (background_id);


--
-- Name: background_skill_proficiency background_skill_proficiency_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_skill_proficiency
    ADD CONSTRAINT background_skill_proficiency_pkey PRIMARY KEY (background_id, skill_id);


--
-- Name: background_tool_proficiency background_tool_proficiency_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_tool_proficiency
    ADD CONSTRAINT background_tool_proficiency_pkey PRIMARY KEY (background_tool_proficiency_id);


--
-- Name: battle_combatants battle_combatants_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.battle_combatants
    ADD CONSTRAINT battle_combatants_pkey PRIMARY KEY (id);


--
-- Name: battles battles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.battles
    ADD CONSTRAINT battles_pkey PRIMARY KEY (id);


--
-- Name: blueprint_locations blueprint_locations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_locations
    ADD CONSTRAINT blueprint_locations_pkey PRIMARY KEY (id);


--
-- Name: blueprint_npcs blueprint_npcs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_npcs
    ADD CONSTRAINT blueprint_npcs_pkey PRIMARY KEY (id);


--
-- Name: blueprint_quest_rewards blueprint_quest_rewards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_quest_rewards
    ADD CONSTRAINT blueprint_quest_rewards_pkey PRIMARY KEY (id);


--
-- Name: blueprint_quests blueprint_quests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_quests
    ADD CONSTRAINT blueprint_quests_pkey PRIMARY KEY (id);


--
-- Name: buffs_debuffs buffs_debuffs_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.buffs_debuffs
    ADD CONSTRAINT buffs_debuffs_name_key UNIQUE (name);


--
-- Name: buffs_debuffs buffs_debuffs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.buffs_debuffs
    ADD CONSTRAINT buffs_debuffs_pkey PRIMARY KEY (id);


--
-- Name: campaign_blueprints campaign_blueprints_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_blueprints
    ADD CONSTRAINT campaign_blueprints_pkey PRIMARY KEY (id);


--
-- Name: campaign_locations campaign_locations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_locations
    ADD CONSTRAINT campaign_locations_pkey PRIMARY KEY (id);


--
-- Name: campaign_members campaign_members_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_members
    ADD CONSTRAINT campaign_members_pkey PRIMARY KEY (id);


--
-- Name: campaign_npc_spells campaign_npc_spells_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_npc_spells
    ADD CONSTRAINT campaign_npc_spells_pkey PRIMARY KEY (npc_id, spell_id);


--
-- Name: campaign_npcs campaign_npcs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_npcs
    ADD CONSTRAINT campaign_npcs_pkey PRIMARY KEY (id);


--
-- Name: campaign_quests campaign_quests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_quests
    ADD CONSTRAINT campaign_quests_pkey PRIMARY KEY (id);


--
-- Name: campaigns campaigns_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT campaigns_pkey PRIMARY KEY (id);


--
-- Name: character_active_effects character_active_effects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_active_effects
    ADD CONSTRAINT character_active_effects_pkey PRIMARY KEY (id);


--
-- Name: character_class character_class_mod_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_class
    ADD CONSTRAINT character_class_mod_id_slug_key UNIQUE (mod_id, slug);


--
-- Name: character_class character_class_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_class
    ADD CONSTRAINT character_class_pkey PRIMARY KEY (class_id);


--
-- Name: character_known_spells character_known_spells_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_known_spells
    ADD CONSTRAINT character_known_spells_pkey PRIMARY KEY (id);


--
-- Name: character_resources character_resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_resources
    ADD CONSTRAINT character_resources_pkey PRIMARY KEY (id);


--
-- Name: character_reward_ability_score_selection character_reward_ability_score_selection_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_ability_score_selection
    ADD CONSTRAINT character_reward_ability_score_selection_pkey PRIMARY KEY (character_reward_selection_id, reward_grant_id, ability_score_id);


--
-- Name: character_reward_selection character_reward_selection_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_selection
    ADD CONSTRAINT character_reward_selection_pkey PRIMARY KEY (character_reward_selection_id);


--
-- Name: character_reward_skill_selection character_reward_skill_selection_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_skill_selection
    ADD CONSTRAINT character_reward_skill_selection_pkey PRIMARY KEY (character_reward_selection_id, reward_grant_id, skill_id);


--
-- Name: character_reward_spell_selection character_reward_spell_selection_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_spell_selection
    ADD CONSTRAINT character_reward_spell_selection_pkey PRIMARY KEY (character_reward_selection_id, reward_grant_id, spell_id);


--
-- Name: character_size character_size_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_size
    ADD CONSTRAINT character_size_pkey PRIMARY KEY (character_size_id);


--
-- Name: character_skill_proficiencies character_skill_proficiencies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_skill_proficiencies
    ADD CONSTRAINT character_skill_proficiencies_pkey PRIMARY KEY (id);


--
-- Name: character_spell_slot_usage character_spell_slot_usage_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_spell_slot_usage
    ADD CONSTRAINT character_spell_slot_usage_pkey PRIMARY KEY (character_id, spell_level);


--
-- Name: character_stats character_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_stats
    ADD CONSTRAINT character_stats_pkey PRIMARY KEY (id);


--
-- Name: character_wallets character_wallets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_wallets
    ADD CONSTRAINT character_wallets_pkey PRIMARY KEY (id);


--
-- Name: characters characters_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.characters
    ADD CONSTRAINT characters_pkey PRIMARY KEY (id);


--
-- Name: class_authoring_idempotency class_authoring_idempotency_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_authoring_idempotency
    ADD CONSTRAINT class_authoring_idempotency_pkey PRIMARY KEY (id);


--
-- Name: class_feature class_feature_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_feature
    ADD CONSTRAINT class_feature_pkey PRIMARY KEY (class_feature_id);


--
-- Name: class_level_reward_grant_ability_option class_level_reward_grant_ability_option_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_ability_option
    ADD CONSTRAINT class_level_reward_grant_ability_option_pkey PRIMARY KEY (reward_grant_id, ability_score_id);


--
-- Name: class_level_reward_grant_ability_score class_level_reward_grant_ability_score_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_ability_score
    ADD CONSTRAINT class_level_reward_grant_ability_score_pkey PRIMARY KEY (reward_grant_id);


--
-- Name: class_level_reward_grant_custom_text class_level_reward_grant_custom_text_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_custom_text
    ADD CONSTRAINT class_level_reward_grant_custom_text_pkey PRIMARY KEY (reward_grant_id);


--
-- Name: class_level_reward_grant_feat class_level_reward_grant_feat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_feat
    ADD CONSTRAINT class_level_reward_grant_feat_pkey PRIMARY KEY (reward_grant_id);


--
-- Name: class_level_reward_grant_feature class_level_reward_grant_feature_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_feature
    ADD CONSTRAINT class_level_reward_grant_feature_pkey PRIMARY KEY (reward_grant_id);


--
-- Name: class_level_reward_grant_numeric_modifier class_level_reward_grant_numeric_modifier_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_numeric_modifier
    ADD CONSTRAINT class_level_reward_grant_numeric_modifier_pkey PRIMARY KEY (reward_grant_id);


--
-- Name: class_level_reward_grant class_level_reward_grant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant
    ADD CONSTRAINT class_level_reward_grant_pkey PRIMARY KEY (reward_grant_id);


--
-- Name: class_level_reward_grant_skill_option class_level_reward_grant_skill_option_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_skill_option
    ADD CONSTRAINT class_level_reward_grant_skill_option_pkey PRIMARY KEY (reward_grant_id, skill_id);


--
-- Name: class_level_reward_grant_skill_proficiency class_level_reward_grant_skill_proficiency_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_skill_proficiency
    ADD CONSTRAINT class_level_reward_grant_skill_proficiency_pkey PRIMARY KEY (reward_grant_id);


--
-- Name: class_level_reward_grant_spell class_level_reward_grant_spell_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_spell
    ADD CONSTRAINT class_level_reward_grant_spell_pkey PRIMARY KEY (reward_grant_id);


--
-- Name: class_level_reward_grant_subclass class_level_reward_grant_subclass_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_subclass
    ADD CONSTRAINT class_level_reward_grant_subclass_pkey PRIMARY KEY (reward_grant_id);


--
-- Name: class_level_reward_group class_level_reward_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_group
    ADD CONSTRAINT class_level_reward_group_pkey PRIMARY KEY (reward_group_id);


--
-- Name: class_level_reward_option class_level_reward_option_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_option
    ADD CONSTRAINT class_level_reward_option_pkey PRIMARY KEY (reward_option_id);


--
-- Name: class_level_reward_option class_level_reward_option_reward_option_id_reward_group_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_option
    ADD CONSTRAINT class_level_reward_option_reward_option_id_reward_group_id_key UNIQUE (reward_option_id, reward_group_id);


--
-- Name: class_primary_ability class_primary_ability_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_primary_ability
    ADD CONSTRAINT class_primary_ability_pkey PRIMARY KEY (class_id, ability_score_id);


--
-- Name: class_progression_column class_progression_column_class_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_progression_column
    ADD CONSTRAINT class_progression_column_class_id_slug_key UNIQUE (class_id, slug);


--
-- Name: class_progression_column class_progression_column_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_progression_column
    ADD CONSTRAINT class_progression_column_pkey PRIMARY KEY (class_progression_column_id);


--
-- Name: class_progression_value class_progression_value_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_progression_value
    ADD CONSTRAINT class_progression_value_pkey PRIMARY KEY (class_id, class_level, class_progression_column_id);


--
-- Name: class_saving_throw class_saving_throw_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_saving_throw
    ADD CONSTRAINT class_saving_throw_pkey PRIMARY KEY (class_id, ability_score_id);


--
-- Name: class_skill_option class_skill_option_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_skill_option
    ADD CONSTRAINT class_skill_option_pkey PRIMARY KEY (class_id, skill_id);


--
-- Name: creature_type creature_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.creature_type
    ADD CONSTRAINT creature_type_pkey PRIMARY KEY (creature_type_id);


--
-- Name: creature_type creature_type_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.creature_type
    ADD CONSTRAINT creature_type_slug_key UNIQUE (slug);


--
-- Name: currency currency_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.currency
    ADD CONSTRAINT currency_pkey PRIMARY KEY (currency_id);


--
-- Name: custom_resource_types custom_resource_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_resource_types
    ADD CONSTRAINT custom_resource_types_pkey PRIMARY KEY (id);


--
-- Name: damage_type damage_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_type
    ADD CONSTRAINT damage_type_pkey PRIMARY KEY (damage_type_id);


--
-- Name: damage_types damage_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_types
    ADD CONSTRAINT damage_types_pkey PRIMARY KEY (id);


--
-- Name: dice_formula dice_formula_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dice_formula
    ADD CONSTRAINT dice_formula_pkey PRIMARY KEY (dice_formula_id);


--
-- Name: enchantment_types enchantment_types_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enchantment_types
    ADD CONSTRAINT enchantment_types_name_key UNIQUE (name);


--
-- Name: enchantment_types enchantment_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enchantment_types
    ADD CONSTRAINT enchantment_types_pkey PRIMARY KEY (id);


--
-- Name: equipment_category equipment_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_category
    ADD CONSTRAINT equipment_category_pkey PRIMARY KEY (equipment_category_id);


--
-- Name: equipment_item equipment_item_mod_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_item
    ADD CONSTRAINT equipment_item_mod_id_slug_key UNIQUE (mod_id, slug);


--
-- Name: equipment_item equipment_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_item
    ADD CONSTRAINT equipment_item_pkey PRIMARY KEY (equipment_item_id);


--
-- Name: equipment_slots equipment_slots_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_slots
    ADD CONSTRAINT equipment_slots_pkey PRIMARY KEY (id);


--
-- Name: feat_category feat_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat_category
    ADD CONSTRAINT feat_category_pkey PRIMARY KEY (feat_category_id);


--
-- Name: feat_category feat_category_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat_category
    ADD CONSTRAINT feat_category_slug_key UNIQUE (slug);


--
-- Name: feat feat_mod_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat
    ADD CONSTRAINT feat_mod_id_slug_key UNIQUE (mod_id, slug);


--
-- Name: feat feat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat
    ADD CONSTRAINT feat_pkey PRIMARY KEY (feat_id);


--
-- Name: feat_prerequisite feat_prerequisite_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat_prerequisite
    ADD CONSTRAINT feat_prerequisite_pkey PRIMARY KEY (feat_prerequisite_id);


--
-- Name: feat_section feat_section_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat_section
    ADD CONSTRAINT feat_section_pkey PRIMARY KEY (feat_section_id);


--
-- Name: gm_session_notes gm_session_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gm_session_notes
    ADD CONSTRAINT gm_session_notes_pkey PRIMARY KEY (id);


--
-- Name: homebrew_content_items homebrew_content_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_content_items
    ADD CONSTRAINT homebrew_content_items_pkey PRIMARY KEY (id);


--
-- Name: homebrew_content_versions homebrew_content_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_content_versions
    ADD CONSTRAINT homebrew_content_versions_pkey PRIMARY KEY (id);


--
-- Name: homebrew_packages homebrew_packages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_packages
    ADD CONSTRAINT homebrew_packages_pkey PRIMARY KEY (id);


--
-- Name: homebrew_tags homebrew_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_tags
    ADD CONSTRAINT homebrew_tags_pkey PRIMARY KEY (id);


--
-- Name: import_warning import_warning_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_warning
    ADD CONSTRAINT import_warning_pkey PRIMARY KEY (import_warning_id);


--
-- Name: item_enchantments item_enchantments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_enchantments
    ADD CONSTRAINT item_enchantments_pkey PRIMARY KEY (id);


--
-- Name: item_instances item_instances_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_instances
    ADD CONSTRAINT item_instances_pkey PRIMARY KEY (id);


--
-- Name: item_rarities item_rarities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_rarities
    ADD CONSTRAINT item_rarities_pkey PRIMARY KEY (id);


--
-- Name: item_template_buffs item_template_buffs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_template_buffs
    ADD CONSTRAINT item_template_buffs_pkey PRIMARY KEY (id);


--
-- Name: item_templates item_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_templates
    ADD CONSTRAINT item_templates_pkey PRIMARY KEY (id);


--
-- Name: item_types item_types_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_types
    ADD CONSTRAINT item_types_name_key UNIQUE (name);


--
-- Name: item_types item_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_types
    ADD CONSTRAINT item_types_pkey PRIMARY KEY (id);


--
-- Name: magic_item_allowed_equipment magic_item_allowed_equipment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item_allowed_equipment
    ADD CONSTRAINT magic_item_allowed_equipment_pkey PRIMARY KEY (magic_item_id, equipment_item_id);


--
-- Name: magic_item magic_item_mod_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item
    ADD CONSTRAINT magic_item_mod_id_slug_key UNIQUE (mod_id, slug);


--
-- Name: magic_item magic_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item
    ADD CONSTRAINT magic_item_pkey PRIMARY KEY (magic_item_id);


--
-- Name: magic_item_rarity magic_item_rarity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item_rarity
    ADD CONSTRAINT magic_item_rarity_pkey PRIMARY KEY (magic_item_rarity_id);


--
-- Name: magic_item_type magic_item_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item_type
    ADD CONSTRAINT magic_item_type_pkey PRIMARY KEY (magic_item_type_id);


--
-- Name: mod_package mod_package_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mod_package
    ADD CONSTRAINT mod_package_pkey PRIMARY KEY (mod_id);


--
-- Name: mod_package mod_package_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mod_package
    ADD CONSTRAINT mod_package_slug_key UNIQUE (slug);


--
-- Name: money_value money_value_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.money_value
    ADD CONSTRAINT money_value_pkey PRIMARY KEY (money_value_id);


--
-- Name: npc_notes npc_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.npc_notes
    ADD CONSTRAINT npc_notes_pkey PRIMARY KEY (id);


--
-- Name: blueprint_homebrew pk_blueprint_homebrew; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_homebrew
    ADD CONSTRAINT pk_blueprint_homebrew PRIMARY KEY (blueprint_id, package_id);


--
-- Name: blueprint_npc_spells pk_blueprint_npc_spells; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_npc_spells
    ADD CONSTRAINT pk_blueprint_npc_spells PRIMARY KEY (npc_id, spell_id);


--
-- Name: campaign_homebrew pk_campaign_homebrew; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_homebrew
    ADD CONSTRAINT pk_campaign_homebrew PRIMARY KEY (campaign_id, package_id);


--
-- Name: character_class_levels pk_character_class_levels; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_class_levels
    ADD CONSTRAINT pk_character_class_levels PRIMARY KEY (character_id, class_id);


--
-- Name: gm_homebrew_library pk_gm_homebrew_library; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gm_homebrew_library
    ADD CONSTRAINT pk_gm_homebrew_library PRIMARY KEY (gm_user_id, package_id);


--
-- Name: homebrew_package_tags pk_homebrew_package_tags; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_package_tags
    ADD CONSTRAINT pk_homebrew_package_tags PRIMARY KEY (package_id, tag_id);


--
-- Name: homebrew_ratings pk_homebrew_ratings; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_ratings
    ADD CONSTRAINT pk_homebrew_ratings PRIMARY KEY (user_id, package_id);


--
-- Name: proficiency_skills proficiency_skills_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.proficiency_skills
    ADD CONSTRAINT proficiency_skills_name_key UNIQUE (name);


--
-- Name: proficiency_skills proficiency_skills_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.proficiency_skills
    ADD CONSTRAINT proficiency_skills_pkey PRIMARY KEY (id);


--
-- Name: quest_items quest_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_items
    ADD CONSTRAINT quest_items_pkey PRIMARY KEY (id);


--
-- Name: quest_locations quest_locations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_locations
    ADD CONSTRAINT quest_locations_pkey PRIMARY KEY (id);


--
-- Name: quest_notes quest_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_notes
    ADD CONSTRAINT quest_notes_pkey PRIMARY KEY (id);


--
-- Name: quest_npcs quest_npcs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_npcs
    ADD CONSTRAINT quest_npcs_pkey PRIMARY KEY (id);


--
-- Name: quest_rewards quest_rewards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_rewards
    ADD CONSTRAINT quest_rewards_pkey PRIMARY KEY (id);


--
-- Name: random_table_entry random_table_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.random_table_entry
    ADD CONSTRAINT random_table_entry_pkey PRIMARY KEY (random_table_entry_id);


--
-- Name: random_table random_table_mod_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.random_table
    ADD CONSTRAINT random_table_mod_id_slug_key UNIQUE (mod_id, slug);


--
-- Name: random_table random_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.random_table
    ADD CONSTRAINT random_table_pkey PRIMARY KEY (random_table_id);


--
-- Name: shared_storage shared_storage_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shared_storage
    ADD CONSTRAINT shared_storage_pkey PRIMARY KEY (id);


--
-- Name: skill_effects skill_effects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skill_effects
    ADD CONSTRAINT skill_effects_pkey PRIMARY KEY (id);


--
-- Name: skill skill_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skill
    ADD CONSTRAINT skill_pkey PRIMARY KEY (skill_id);


--
-- Name: skills skills_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT skills_name_key UNIQUE (name);


--
-- Name: skills skills_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT skills_pkey PRIMARY KEY (id);


--
-- Name: source_book source_book_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.source_book
    ADD CONSTRAINT source_book_pkey PRIMARY KEY (source_id);


--
-- Name: source_book source_book_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.source_book
    ADD CONSTRAINT source_book_slug_key UNIQUE (slug);


--
-- Name: species species_mod_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species
    ADD CONSTRAINT species_mod_id_slug_key UNIQUE (mod_id, slug);


--
-- Name: species species_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species
    ADD CONSTRAINT species_pkey PRIMARY KEY (species_id);


--
-- Name: species_size_option species_size_option_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_size_option
    ADD CONSTRAINT species_size_option_pkey PRIMARY KEY (species_id, character_size_id);


--
-- Name: species_speed species_speed_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_speed
    ADD CONSTRAINT species_speed_pkey PRIMARY KEY (species_speed_id);


--
-- Name: species_trait_effect species_trait_effect_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_trait_effect
    ADD CONSTRAINT species_trait_effect_pkey PRIMARY KEY (species_trait_effect_id);


--
-- Name: species_trait species_trait_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_trait
    ADD CONSTRAINT species_trait_pkey PRIMARY KEY (species_trait_id);


--
-- Name: species_trait species_trait_species_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_trait
    ADD CONSTRAINT species_trait_species_id_slug_key UNIQUE (species_id, slug);


--
-- Name: spell_class spell_class_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_class
    ADD CONSTRAINT spell_class_pkey PRIMARY KEY (spell_id, class_id);


--
-- Name: spell_component spell_component_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_component
    ADD CONSTRAINT spell_component_pkey PRIMARY KEY (spell_id, component_slug);


--
-- Name: spell spell_mod_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell
    ADD CONSTRAINT spell_mod_id_slug_key UNIQUE (mod_id, slug);


--
-- Name: spell spell_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell
    ADD CONSTRAINT spell_pkey PRIMARY KEY (spell_id);


--
-- Name: spell_school spell_school_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_school
    ADD CONSTRAINT spell_school_pkey PRIMARY KEY (spell_school_id);


--
-- Name: spell_school spell_school_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_school
    ADD CONSTRAINT spell_school_slug_key UNIQUE (slug);


--
-- Name: spell_scroll_crafting_rule spell_scroll_crafting_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_scroll_crafting_rule
    ADD CONSTRAINT spell_scroll_crafting_rule_pkey PRIMARY KEY (spell_scroll_crafting_rule_id);


--
-- Name: spell_subclass spell_subclass_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_subclass
    ADD CONSTRAINT spell_subclass_pkey PRIMARY KEY (spell_id, subclass_id);


--
-- Name: subclass subclass_class_id_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subclass
    ADD CONSTRAINT subclass_class_id_slug_key UNIQUE (class_id, slug);


--
-- Name: subclass subclass_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subclass
    ADD CONSTRAINT subclass_pkey PRIMARY KEY (subclass_id);


--
-- Name: character_known_spells uk29ywd2684rvwu8julh0nfgoke; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_known_spells
    ADD CONSTRAINT uk29ywd2684rvwu8julh0nfgoke UNIQUE (character_id, spell_id);


--
-- Name: character_resources uk5b7fy8lo5jamas3fbmma8tdav; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_resources
    ADD CONSTRAINT uk5b7fy8lo5jamas3fbmma8tdav UNIQUE (character_id, resource_type_id);


--
-- Name: quest_locations uk6lvkk8upd7da59f0i184xs4wu; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_locations
    ADD CONSTRAINT uk6lvkk8upd7da59f0i184xs4wu UNIQUE (quest_id, location_id);


--
-- Name: item_template_buffs ukc7yx5r5smq3xxgelbi4o5k4u5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_template_buffs
    ADD CONSTRAINT ukc7yx5r5smq3xxgelbi4o5k4u5 UNIQUE (template_id, buff_debuff_id);


--
-- Name: character_wallets ukmur9nyimn0372tc9p6qum23tp; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_wallets
    ADD CONSTRAINT ukmur9nyimn0372tc9p6qum23tp UNIQUE (character_id, currency_type_id);


--
-- Name: campaign_members uko1xh7fspormu5sb6vtw39u3we; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_members
    ADD CONSTRAINT uko1xh7fspormu5sb6vtw39u3we UNIQUE (campaign_id, user_id);


--
-- Name: item_enchantments ukps7detv2in7vjel885bd2qlbb; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_enchantments
    ADD CONSTRAINT ukps7detv2in7vjel885bd2qlbb UNIQUE (item_instance_id, enchantment_type_id);


--
-- Name: quest_npcs ukr12yu9bwrslxntfpct7oq26to; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_npcs
    ADD CONSTRAINT ukr12yu9bwrslxntfpct7oq26to UNIQUE (quest_id, npc_id);


--
-- Name: character_skill_proficiencies uktf891j9n53ympit8reb81twtp; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_skill_proficiencies
    ADD CONSTRAINT uktf891j9n53ympit8reb81twtp UNIQUE (character_id, skill_id);


--
-- Name: universes universes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.universes
    ADD CONSTRAINT universes_pkey PRIMARY KEY (id);


--
-- Name: campaigns uq_campaign_invite_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT uq_campaign_invite_code UNIQUE (invite_code);


--
-- Name: campaign_members uq_campaign_member; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_members
    ADD CONSTRAINT uq_campaign_member UNIQUE (campaign_id, user_id);


--
-- Name: character_skill_proficiencies uq_char_profskill; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_skill_proficiencies
    ADD CONSTRAINT uq_char_profskill UNIQUE (character_id, skill_id);


--
-- Name: character_known_spells uq_char_spell; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_known_spells
    ADD CONSTRAINT uq_char_spell UNIQUE (character_id, spell_id);


--
-- Name: character_stats uq_char_stat; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_stats
    ADD CONSTRAINT uq_char_stat UNIQUE (character_id, stat_type_id);


--
-- Name: character_resources uq_charres_char_restype; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_resources
    ADD CONSTRAINT uq_charres_char_restype UNIQUE (character_id, resource_type_id);


--
-- Name: class_authoring_idempotency uq_class_authoring_idem; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_authoring_idempotency
    ADD CONSTRAINT uq_class_authoring_idem UNIQUE (scope, idem_key);


--
-- Name: homebrew_content_items uq_homebrew_content_item; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_content_items
    ADD CONSTRAINT uq_homebrew_content_item UNIQUE (package_id, content_type, content_id);


--
-- Name: homebrew_tags uq_homebrew_tag_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_tags
    ADD CONSTRAINT uq_homebrew_tag_name UNIQUE (name);


--
-- Name: item_enchantments uq_item_enchantment; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_enchantments
    ADD CONSTRAINT uq_item_enchantment UNIQUE (item_instance_id, enchantment_type_id);


--
-- Name: item_template_buffs uq_itemtpl_buff; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_template_buffs
    ADD CONSTRAINT uq_itemtpl_buff UNIQUE (template_id, buff_debuff_id);


--
-- Name: quest_items uq_quest_item; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_items
    ADD CONSTRAINT uq_quest_item UNIQUE (quest_id, item_template_id);


--
-- Name: quest_locations uq_quest_location; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_locations
    ADD CONSTRAINT uq_quest_location UNIQUE (quest_id, location_id);


--
-- Name: quest_npcs uq_quest_npc; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_npcs
    ADD CONSTRAINT uq_quest_npc UNIQUE (quest_id, npc_id);


--
-- Name: skill_effects uq_skill_buff; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skill_effects
    ADD CONSTRAINT uq_skill_buff UNIQUE (skill_id, buff_debuff_id);


--
-- Name: skill_effects uq_skill_buffdebuff; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skill_effects
    ADD CONSTRAINT uq_skill_buffdebuff UNIQUE (skill_id, buff_debuff_id);


--
-- Name: universes uq_universe_slug; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.universes
    ADD CONSTRAINT uq_universe_slug UNIQUE (slug);


--
-- Name: character_wallets uq_wallet_char_currency; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_wallets
    ADD CONSTRAINT uq_wallet_char_currency UNIQUE (character_id, currency_type_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: wallet_transactions wallet_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wallet_transactions
    ADD CONSTRAINT wallet_transactions_pkey PRIMARY KEY (id);


--
-- Name: weapon_item_property weapon_item_property_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_item_property
    ADD CONSTRAINT weapon_item_property_pkey PRIMARY KEY (equipment_item_id, weapon_property_id);


--
-- Name: weapon_mastery weapon_mastery_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_mastery
    ADD CONSTRAINT weapon_mastery_pkey PRIMARY KEY (weapon_mastery_id);


--
-- Name: weapon_mastery weapon_mastery_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_mastery
    ADD CONSTRAINT weapon_mastery_slug_key UNIQUE (slug);


--
-- Name: weapon_property weapon_property_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_property
    ADD CONSTRAINT weapon_property_pkey PRIMARY KEY (weapon_property_id);


--
-- Name: weapon_property weapon_property_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_property
    ADD CONSTRAINT weapon_property_slug_key UNIQUE (slug);


--
-- Name: weapon_stat weapon_stat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_stat
    ADD CONSTRAINT weapon_stat_pkey PRIMARY KEY (equipment_item_id);


--
-- Name: idx_battle_campaign_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_battle_campaign_id ON public.battles USING btree (campaign_id);


--
-- Name: idx_bloc_blueprint_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bloc_blueprint_id ON public.blueprint_locations USING btree (blueprint_id);


--
-- Name: idx_blueprint_author_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_blueprint_author_id ON public.campaign_blueprints USING btree (author_id);


--
-- Name: idx_blueprint_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_blueprint_status ON public.campaign_blueprints USING btree (status);


--
-- Name: idx_blueprint_universe_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_blueprint_universe_id ON public.campaign_blueprints USING btree (universe_id);


--
-- Name: idx_bnpc_blueprint_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bnpc_blueprint_id ON public.blueprint_npcs USING btree (blueprint_id);


--
-- Name: idx_bquest_blueprint_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bquest_blueprint_id ON public.blueprint_quests USING btree (blueprint_id);


--
-- Name: idx_buffdebuff_homebrew_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_buffdebuff_homebrew_id ON public.buffs_debuffs USING btree (homebrew_id);


--
-- Name: idx_cae_buff_debuff_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cae_buff_debuff_id ON public.character_active_effects USING btree (buff_debuff_id);


--
-- Name: idx_cae_character_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cae_character_id ON public.character_active_effects USING btree (character_id);


--
-- Name: idx_campaigns_invite_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_campaigns_invite_code ON public.campaigns USING btree (invite_code);


--
-- Name: idx_campaigns_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_campaigns_status ON public.campaigns USING btree (status);


--
-- Name: idx_character_reward_selection_character; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_character_reward_selection_character ON public.character_reward_selection USING btree (character_id);


--
-- Name: idx_characters_blueprint_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_characters_blueprint_id ON public.characters USING btree (blueprint_id);


--
-- Name: idx_characters_campaign_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_characters_campaign_id ON public.characters USING btree (campaign_id);


--
-- Name: idx_characters_owner_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_characters_owner_id ON public.characters USING btree (owner_id);


--
-- Name: idx_characters_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_characters_status ON public.characters USING btree (status);


--
-- Name: idx_class_authoring_idem_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_class_authoring_idem_created ON public.class_authoring_idempotency USING btree (created_at);


--
-- Name: idx_class_reward_grant_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_class_reward_grant_group ON public.class_level_reward_grant USING btree (reward_group_id);


--
-- Name: idx_class_reward_grant_option; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_class_reward_grant_option ON public.class_level_reward_grant USING btree (reward_option_id);


--
-- Name: idx_class_reward_group_class_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_class_reward_group_class_level ON public.class_level_reward_group USING btree (class_id, class_level, sort_order);


--
-- Name: idx_cm_campaign_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cm_campaign_id ON public.campaign_members USING btree (campaign_id);


--
-- Name: idx_cm_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cm_user_id ON public.campaign_members USING btree (user_id);


--
-- Name: idx_combatant_battle_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_combatant_battle_id ON public.battle_combatants USING btree (battle_id);


--
-- Name: idx_content_ability_score_hb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_ability_score_hb ON public.ability_score USING btree (homebrew_id);


--
-- Name: idx_content_background_name_ru; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_background_name_ru ON public.background USING gin (to_tsvector('russian'::regconfig, name_ru));


--
-- Name: idx_content_character_size_hb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_character_size_hb ON public.character_size USING btree (homebrew_id);


--
-- Name: idx_content_currency_hb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_currency_hb ON public.currency USING btree (homebrew_id);


--
-- Name: idx_content_damage_type_hb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_damage_type_hb ON public.damage_type USING btree (homebrew_id);


--
-- Name: idx_content_equipment_category_hb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_equipment_category_hb ON public.equipment_category USING btree (homebrew_id);


--
-- Name: idx_content_equipment_item_kind; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_equipment_item_kind ON public.equipment_item USING btree (kind);


--
-- Name: idx_content_equipment_name_ru; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_equipment_name_ru ON public.equipment_item USING gin (to_tsvector('russian'::regconfig, name_ru));


--
-- Name: idx_content_magic_item_rarity_hb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_magic_item_rarity_hb ON public.magic_item_rarity USING btree (homebrew_id);


--
-- Name: idx_content_magic_item_type_hb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_magic_item_type_hb ON public.magic_item_type USING btree (homebrew_id);


--
-- Name: idx_content_magic_item_type_rarity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_magic_item_type_rarity ON public.magic_item USING btree (magic_item_type_id, rarity_id);


--
-- Name: idx_content_skill_hb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_skill_hb ON public.skill USING btree (homebrew_id);


--
-- Name: idx_content_spell_level_school; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_spell_level_school ON public.spell USING btree (level, school_id);


--
-- Name: idx_content_spell_name_ru; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_spell_name_ru ON public.spell USING gin (to_tsvector('russian'::regconfig, name_ru));


--
-- Name: idx_enchtype_homebrew_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_enchtype_homebrew_id ON public.enchantment_types USING btree (homebrew_id);


--
-- Name: idx_gmnotes_campaign_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gmnotes_campaign_id ON public.gm_session_notes USING btree (campaign_id);


--
-- Name: idx_homebrew_author_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_homebrew_author_id ON public.homebrew_packages USING btree (author_id);


--
-- Name: idx_homebrew_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_homebrew_status ON public.homebrew_packages USING btree (status);


--
-- Name: idx_iteminst_equipment_item_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_iteminst_equipment_item_id ON public.item_instances USING btree (equipment_item_id);


--
-- Name: idx_iteminst_magic_item_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_iteminst_magic_item_id ON public.item_instances USING btree (magic_item_id);


--
-- Name: idx_iteminst_owner_character_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_iteminst_owner_character_id ON public.item_instances USING btree (owner_character_id);


--
-- Name: idx_iteminst_shared_storage_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_iteminst_shared_storage_id ON public.item_instances USING btree (shared_storage_id);


--
-- Name: idx_iteminst_template_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_iteminst_template_id ON public.item_instances USING btree (template_id);


--
-- Name: idx_itemtpl_homebrew_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_itemtpl_homebrew_id ON public.item_templates USING btree (homebrew_id);


--
-- Name: idx_itemtype_homebrew_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_itemtype_homebrew_id ON public.item_types USING btree (homebrew_id);


--
-- Name: idx_location_campaign_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_location_campaign_id ON public.campaign_locations USING btree (campaign_id);


--
-- Name: idx_location_visible; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_location_visible ON public.campaign_locations USING btree (is_visible_to_players);


--
-- Name: idx_npc_campaign_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_npc_campaign_id ON public.campaign_npcs USING btree (campaign_id);


--
-- Name: idx_npc_class_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_npc_class_id ON public.campaign_npcs USING btree (class_id);


--
-- Name: idx_npc_race_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_npc_race_id ON public.campaign_npcs USING btree (race_id);


--
-- Name: idx_npc_source_monster_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_npc_source_monster_id ON public.campaign_npcs USING btree (source_monster_id);


--
-- Name: idx_npc_spells_spell_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_npc_spells_spell_id ON public.campaign_npc_spells USING btree (spell_id);


--
-- Name: idx_npc_visible; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_npc_visible ON public.campaign_npcs USING btree (is_visible_to_players);


--
-- Name: idx_quest_campaign_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quest_campaign_id ON public.campaign_quests USING btree (campaign_id);


--
-- Name: idx_quest_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quest_status ON public.campaign_quests USING btree (status);


--
-- Name: idx_quest_visible; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quest_visible ON public.campaign_quests USING btree (is_visible_to_players);


--
-- Name: idx_restype_homebrew_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_restype_homebrew_id ON public.custom_resource_types USING btree (homebrew_id);


--
-- Name: idx_skill_homebrew_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_skill_homebrew_id ON public.skills USING btree (homebrew_id);


--
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role ON public.users USING btree (role);


--
-- Name: idx_wallet_tx_character_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wallet_tx_character_created ON public.wallet_transactions USING btree (character_id, created_at DESC);


--
-- Name: uq_ability_score_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_ability_score_hb_slug ON public.ability_score USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_ability_score_vanilla_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_ability_score_vanilla_slug ON public.ability_score USING btree (slug) WHERE (homebrew_id IS NULL);


--
-- Name: uq_background_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_background_hb_slug ON public.background USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_character_class_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_character_class_hb_slug ON public.character_class USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_character_reward_selection_option; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_character_reward_selection_option ON public.character_reward_selection USING btree (character_id, reward_group_id, reward_option_id);


--
-- Name: uq_character_size_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_character_size_hb_slug ON public.character_size USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_character_size_vanilla_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_character_size_vanilla_slug ON public.character_size USING btree (slug) WHERE (homebrew_id IS NULL);


--
-- Name: uq_class_feature_base_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_class_feature_base_slug ON public.class_feature USING btree (class_id, slug) WHERE (subclass_id IS NULL);


--
-- Name: uq_class_feature_subclass_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_class_feature_subclass_slug ON public.class_feature USING btree (subclass_id, slug) WHERE (subclass_id IS NOT NULL);


--
-- Name: uq_class_reward_option_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_class_reward_option_key ON public.class_level_reward_option USING btree (reward_group_id, option_key) WHERE (option_key IS NOT NULL);


--
-- Name: uq_currency_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_currency_hb_slug ON public.currency USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_currency_vanilla_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_currency_vanilla_slug ON public.currency USING btree (slug) WHERE (homebrew_id IS NULL);


--
-- Name: uq_damage_type_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_damage_type_hb_slug ON public.damage_type USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_damage_type_vanilla_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_damage_type_vanilla_slug ON public.damage_type USING btree (slug) WHERE (homebrew_id IS NULL);


--
-- Name: uq_damage_types_code_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_damage_types_code_owner ON public.damage_types USING btree (code, homebrew_id);


--
-- Name: uq_damage_types_code_system; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_damage_types_code_system ON public.damage_types USING btree (code) WHERE (homebrew_id IS NULL);


--
-- Name: uq_equipment_category_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_equipment_category_hb_slug ON public.equipment_category USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_equipment_category_vanilla_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_equipment_category_vanilla_slug ON public.equipment_category USING btree (slug) WHERE (homebrew_id IS NULL);


--
-- Name: uq_equipment_item_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_equipment_item_hb_slug ON public.equipment_item USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_equipment_slots_code_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_equipment_slots_code_owner ON public.equipment_slots USING btree (code, homebrew_id);


--
-- Name: uq_equipment_slots_code_system; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_equipment_slots_code_system ON public.equipment_slots USING btree (code) WHERE (homebrew_id IS NULL);


--
-- Name: uq_feat_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_feat_hb_slug ON public.feat USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_item_rarities_code_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_item_rarities_code_owner ON public.item_rarities USING btree (code, homebrew_id);


--
-- Name: uq_item_rarities_code_system; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_item_rarities_code_system ON public.item_rarities USING btree (code) WHERE (homebrew_id IS NULL);


--
-- Name: uq_magic_item_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_magic_item_hb_slug ON public.magic_item USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_magic_item_rarity_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_magic_item_rarity_hb_slug ON public.magic_item_rarity USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_magic_item_rarity_vanilla_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_magic_item_rarity_vanilla_slug ON public.magic_item_rarity USING btree (slug) WHERE (homebrew_id IS NULL);


--
-- Name: uq_magic_item_type_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_magic_item_type_hb_slug ON public.magic_item_type USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_magic_item_type_vanilla_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_magic_item_type_vanilla_slug ON public.magic_item_type USING btree (slug) WHERE (homebrew_id IS NULL);


--
-- Name: uq_skill_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_skill_hb_slug ON public.skill USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_skill_vanilla_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_skill_vanilla_slug ON public.skill USING btree (slug) WHERE (homebrew_id IS NULL);


--
-- Name: uq_species_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_species_hb_slug ON public.species USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_spell_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_spell_hb_slug ON public.spell USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: uq_subclass_hb_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_subclass_hb_slug ON public.subclass USING btree (homebrew_id, slug) WHERE (homebrew_id IS NOT NULL);


--
-- Name: ability_score ability_score_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ability_score
    ADD CONSTRAINT ability_score_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: armor_stat armor_stat_equipment_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.armor_stat
    ADD CONSTRAINT armor_stat_equipment_item_id_fkey FOREIGN KEY (equipment_item_id) REFERENCES public.equipment_item(equipment_item_id) ON DELETE CASCADE;


--
-- Name: background_ability_option background_ability_option_ability_score_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_ability_option
    ADD CONSTRAINT background_ability_option_ability_score_id_fkey FOREIGN KEY (ability_score_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: background_ability_option background_ability_option_background_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_ability_option
    ADD CONSTRAINT background_ability_option_background_id_fkey FOREIGN KEY (background_id) REFERENCES public.background(background_id) ON DELETE CASCADE;


--
-- Name: background_equipment_choice_group background_equipment_choice_group_background_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_equipment_choice_group
    ADD CONSTRAINT background_equipment_choice_group_background_id_fkey FOREIGN KEY (background_id) REFERENCES public.background(background_id) ON DELETE CASCADE;


--
-- Name: background_equipment_entry background_equipment_entry_background_equipment_option_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_equipment_entry
    ADD CONSTRAINT background_equipment_entry_background_equipment_option_id_fkey FOREIGN KEY (background_equipment_option_id) REFERENCES public.background_equipment_option(background_equipment_option_id) ON DELETE CASCADE;


--
-- Name: background_equipment_entry background_equipment_entry_equipment_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_equipment_entry
    ADD CONSTRAINT background_equipment_entry_equipment_item_id_fkey FOREIGN KEY (equipment_item_id) REFERENCES public.equipment_item(equipment_item_id);


--
-- Name: background_equipment_entry background_equipment_entry_money_value_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_equipment_entry
    ADD CONSTRAINT background_equipment_entry_money_value_id_fkey FOREIGN KEY (money_value_id) REFERENCES public.money_value(money_value_id);


--
-- Name: background_equipment_option background_equipment_option_background_equipment_choice_gr_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_equipment_option
    ADD CONSTRAINT background_equipment_option_background_equipment_choice_gr_fkey FOREIGN KEY (background_equipment_choice_group_id) REFERENCES public.background_equipment_choice_group(background_equipment_choice_group_id) ON DELETE CASCADE;


--
-- Name: background background_feat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background
    ADD CONSTRAINT background_feat_id_fkey FOREIGN KEY (feat_id) REFERENCES public.feat(feat_id);


--
-- Name: background_feat_option background_feat_option_background_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_feat_option
    ADD CONSTRAINT background_feat_option_background_id_fkey FOREIGN KEY (background_id) REFERENCES public.background(background_id) ON DELETE CASCADE;


--
-- Name: background_feat_option background_feat_option_feat_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_feat_option
    ADD CONSTRAINT background_feat_option_feat_category_id_fkey FOREIGN KEY (feat_category_id) REFERENCES public.feat_category(feat_category_id);


--
-- Name: background_feat_option background_feat_option_feat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_feat_option
    ADD CONSTRAINT background_feat_option_feat_id_fkey FOREIGN KEY (feat_id) REFERENCES public.feat(feat_id);


--
-- Name: background_feat_option background_feat_option_recommended_feat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_feat_option
    ADD CONSTRAINT background_feat_option_recommended_feat_id_fkey FOREIGN KEY (recommended_feat_id) REFERENCES public.feat(feat_id);


--
-- Name: background background_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background
    ADD CONSTRAINT background_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: background_language_proficiency background_language_proficiency_background_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_language_proficiency
    ADD CONSTRAINT background_language_proficiency_background_id_fkey FOREIGN KEY (background_id) REFERENCES public.background(background_id) ON DELETE CASCADE;


--
-- Name: background background_mod_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background
    ADD CONSTRAINT background_mod_id_fkey FOREIGN KEY (mod_id) REFERENCES public.mod_package(mod_id);


--
-- Name: background_skill_proficiency background_skill_proficiency_background_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_skill_proficiency
    ADD CONSTRAINT background_skill_proficiency_background_id_fkey FOREIGN KEY (background_id) REFERENCES public.background(background_id) ON DELETE CASCADE;


--
-- Name: background_skill_proficiency background_skill_proficiency_skill_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_skill_proficiency
    ADD CONSTRAINT background_skill_proficiency_skill_id_fkey FOREIGN KEY (skill_id) REFERENCES public.skill(skill_id);


--
-- Name: background background_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background
    ADD CONSTRAINT background_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.source_book(source_id);


--
-- Name: background_tool_proficiency background_tool_proficiency_background_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_tool_proficiency
    ADD CONSTRAINT background_tool_proficiency_background_id_fkey FOREIGN KEY (background_id) REFERENCES public.background(background_id) ON DELETE CASCADE;


--
-- Name: background_tool_proficiency background_tool_proficiency_equipment_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.background_tool_proficiency
    ADD CONSTRAINT background_tool_proficiency_equipment_item_id_fkey FOREIGN KEY (equipment_item_id) REFERENCES public.equipment_item(equipment_item_id);


--
-- Name: campaign_npc_spells campaign_npc_spells_npc_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_npc_spells
    ADD CONSTRAINT campaign_npc_spells_npc_id_fkey FOREIGN KEY (npc_id) REFERENCES public.campaign_npcs(id) ON DELETE CASCADE;


--
-- Name: campaign_npcs campaign_npcs_source_monster_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--



--
-- Name: character_class character_class_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_class
    ADD CONSTRAINT character_class_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: character_class character_class_mod_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_class
    ADD CONSTRAINT character_class_mod_id_fkey FOREIGN KEY (mod_id) REFERENCES public.mod_package(mod_id);


--
-- Name: character_class character_class_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_class
    ADD CONSTRAINT character_class_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.source_book(source_id);


--
-- Name: character_class character_class_spellcasting_ability_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_class
    ADD CONSTRAINT character_class_spellcasting_ability_id_fkey FOREIGN KEY (spellcasting_ability_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: character_reward_ability_score_selection character_reward_ability_scor_character_reward_selection_i_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_ability_score_selection
    ADD CONSTRAINT character_reward_ability_scor_character_reward_selection_i_fkey FOREIGN KEY (character_reward_selection_id) REFERENCES public.character_reward_selection(character_reward_selection_id) ON DELETE CASCADE;


--
-- Name: character_reward_ability_score_selection character_reward_ability_score_selection_ability_score_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_ability_score_selection
    ADD CONSTRAINT character_reward_ability_score_selection_ability_score_id_fkey FOREIGN KEY (ability_score_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: character_reward_ability_score_selection character_reward_ability_score_selection_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_ability_score_selection
    ADD CONSTRAINT character_reward_ability_score_selection_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant_ability_score(reward_grant_id);


--
-- Name: character_reward_selection character_reward_selection_character_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_selection
    ADD CONSTRAINT character_reward_selection_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: character_reward_selection character_reward_selection_reward_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_selection
    ADD CONSTRAINT character_reward_selection_reward_group_id_fkey FOREIGN KEY (reward_group_id) REFERENCES public.class_level_reward_group(reward_group_id) ON DELETE CASCADE;


--
-- Name: character_reward_skill_selection character_reward_skill_select_character_reward_selection_i_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_skill_selection
    ADD CONSTRAINT character_reward_skill_select_character_reward_selection_i_fkey FOREIGN KEY (character_reward_selection_id) REFERENCES public.character_reward_selection(character_reward_selection_id) ON DELETE CASCADE;


--
-- Name: character_reward_skill_selection character_reward_skill_selection_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_skill_selection
    ADD CONSTRAINT character_reward_skill_selection_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant_skill_proficiency(reward_grant_id);


--
-- Name: character_reward_skill_selection character_reward_skill_selection_skill_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_skill_selection
    ADD CONSTRAINT character_reward_skill_selection_skill_id_fkey FOREIGN KEY (skill_id) REFERENCES public.skill(skill_id);


--
-- Name: character_reward_spell_selection character_reward_spell_select_character_reward_selection_i_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_spell_selection
    ADD CONSTRAINT character_reward_spell_select_character_reward_selection_i_fkey FOREIGN KEY (character_reward_selection_id) REFERENCES public.character_reward_selection(character_reward_selection_id) ON DELETE CASCADE;


--
-- Name: character_reward_spell_selection character_reward_spell_selection_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_spell_selection
    ADD CONSTRAINT character_reward_spell_selection_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant_spell(reward_grant_id);


--
-- Name: character_reward_spell_selection character_reward_spell_selection_spell_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_spell_selection
    ADD CONSTRAINT character_reward_spell_selection_spell_id_fkey FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id);


--
-- Name: character_size character_size_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_size
    ADD CONSTRAINT character_size_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: character_spell_slot_usage character_spell_slot_usage_character_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_spell_slot_usage
    ADD CONSTRAINT character_spell_slot_usage_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: class_feature class_feature_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_feature
    ADD CONSTRAINT class_feature_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.character_class(class_id) ON DELETE CASCADE;


--
-- Name: class_feature class_feature_subclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_feature
    ADD CONSTRAINT class_feature_subclass_id_fkey FOREIGN KEY (subclass_id) REFERENCES public.subclass(subclass_id);


--
-- Name: class_level_reward_grant_ability_option class_level_reward_grant_ability_option_ability_score_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_ability_option
    ADD CONSTRAINT class_level_reward_grant_ability_option_ability_score_id_fkey FOREIGN KEY (ability_score_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: class_level_reward_grant_ability_option class_level_reward_grant_ability_option_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_ability_option
    ADD CONSTRAINT class_level_reward_grant_ability_option_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant_ability_score(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_ability_score class_level_reward_grant_ability_score_ability_score_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_ability_score
    ADD CONSTRAINT class_level_reward_grant_ability_score_ability_score_id_fkey FOREIGN KEY (ability_score_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: class_level_reward_grant_ability_score class_level_reward_grant_ability_score_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_ability_score
    ADD CONSTRAINT class_level_reward_grant_ability_score_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_custom_text class_level_reward_grant_custom_text_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_custom_text
    ADD CONSTRAINT class_level_reward_grant_custom_text_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_feat class_level_reward_grant_feat_feat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_feat
    ADD CONSTRAINT class_level_reward_grant_feat_feat_id_fkey FOREIGN KEY (feat_id) REFERENCES public.feat(feat_id);


--
-- Name: class_level_reward_grant_feat class_level_reward_grant_feat_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_feat
    ADD CONSTRAINT class_level_reward_grant_feat_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_feature class_level_reward_grant_feature_class_feature_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_feature
    ADD CONSTRAINT class_level_reward_grant_feature_class_feature_id_fkey FOREIGN KEY (class_feature_id) REFERENCES public.class_feature(class_feature_id);


--
-- Name: class_level_reward_grant_feature class_level_reward_grant_feature_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_feature
    ADD CONSTRAINT class_level_reward_grant_feature_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_numeric_modifier class_level_reward_grant_numeric_modifier_dice_formula_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_numeric_modifier
    ADD CONSTRAINT class_level_reward_grant_numeric_modifier_dice_formula_id_fkey FOREIGN KEY (dice_formula_id) REFERENCES public.dice_formula(dice_formula_id);


--
-- Name: class_level_reward_grant_numeric_modifier class_level_reward_grant_numeric_modifier_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_numeric_modifier
    ADD CONSTRAINT class_level_reward_grant_numeric_modifier_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant class_level_reward_grant_reward_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant
    ADD CONSTRAINT class_level_reward_grant_reward_group_id_fkey FOREIGN KEY (reward_group_id) REFERENCES public.class_level_reward_group(reward_group_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant class_level_reward_grant_reward_option_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant
    ADD CONSTRAINT class_level_reward_grant_reward_option_id_fkey FOREIGN KEY (reward_option_id) REFERENCES public.class_level_reward_option(reward_option_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_skill_option class_level_reward_grant_skill_option_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_skill_option
    ADD CONSTRAINT class_level_reward_grant_skill_option_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant_skill_proficiency(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_skill_option class_level_reward_grant_skill_option_skill_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_skill_option
    ADD CONSTRAINT class_level_reward_grant_skill_option_skill_id_fkey FOREIGN KEY (skill_id) REFERENCES public.skill(skill_id);


--
-- Name: class_level_reward_grant_skill_proficiency class_level_reward_grant_skill_proficiency_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_skill_proficiency
    ADD CONSTRAINT class_level_reward_grant_skill_proficiency_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_skill_proficiency class_level_reward_grant_skill_proficiency_skill_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_skill_proficiency
    ADD CONSTRAINT class_level_reward_grant_skill_proficiency_skill_id_fkey FOREIGN KEY (skill_id) REFERENCES public.skill(skill_id);


--
-- Name: class_level_reward_grant_spell class_level_reward_grant_spell_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_spell
    ADD CONSTRAINT class_level_reward_grant_spell_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_spell class_level_reward_grant_spell_school_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_spell
    ADD CONSTRAINT class_level_reward_grant_spell_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.spell_school(spell_school_id);


--
-- Name: class_level_reward_grant_spell class_level_reward_grant_spell_spell_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_spell
    ADD CONSTRAINT class_level_reward_grant_spell_spell_id_fkey FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id);


--
-- Name: class_level_reward_grant_subclass class_level_reward_grant_subclass_reward_grant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_subclass
    ADD CONSTRAINT class_level_reward_grant_subclass_reward_grant_id_fkey FOREIGN KEY (reward_grant_id) REFERENCES public.class_level_reward_grant(reward_grant_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_grant_subclass class_level_reward_grant_subclass_subclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_grant_subclass
    ADD CONSTRAINT class_level_reward_grant_subclass_subclass_id_fkey FOREIGN KEY (subclass_id) REFERENCES public.subclass(subclass_id);


--
-- Name: class_level_reward_group class_level_reward_group_class_feature_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_group
    ADD CONSTRAINT class_level_reward_group_class_feature_id_fkey FOREIGN KEY (class_feature_id) REFERENCES public.class_feature(class_feature_id) ON DELETE SET NULL;


--
-- Name: class_level_reward_group class_level_reward_group_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_group
    ADD CONSTRAINT class_level_reward_group_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.character_class(class_id) ON DELETE CASCADE;


--
-- Name: class_level_reward_option class_level_reward_option_reward_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_level_reward_option
    ADD CONSTRAINT class_level_reward_option_reward_group_id_fkey FOREIGN KEY (reward_group_id) REFERENCES public.class_level_reward_group(reward_group_id) ON DELETE CASCADE;


--
-- Name: class_primary_ability class_primary_ability_ability_score_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_primary_ability
    ADD CONSTRAINT class_primary_ability_ability_score_id_fkey FOREIGN KEY (ability_score_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: class_primary_ability class_primary_ability_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_primary_ability
    ADD CONSTRAINT class_primary_ability_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.character_class(class_id) ON DELETE CASCADE;


--
-- Name: class_progression_column class_progression_column_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_progression_column
    ADD CONSTRAINT class_progression_column_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.character_class(class_id) ON DELETE CASCADE;


--
-- Name: class_progression_value class_progression_value_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_progression_value
    ADD CONSTRAINT class_progression_value_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.character_class(class_id) ON DELETE CASCADE;


--
-- Name: class_progression_value class_progression_value_class_progression_column_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_progression_value
    ADD CONSTRAINT class_progression_value_class_progression_column_id_fkey FOREIGN KEY (class_progression_column_id) REFERENCES public.class_progression_column(class_progression_column_id) ON DELETE CASCADE;


--
-- Name: class_saving_throw class_saving_throw_ability_score_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_saving_throw
    ADD CONSTRAINT class_saving_throw_ability_score_id_fkey FOREIGN KEY (ability_score_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: class_saving_throw class_saving_throw_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_saving_throw
    ADD CONSTRAINT class_saving_throw_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.character_class(class_id) ON DELETE CASCADE;


--
-- Name: class_skill_option class_skill_option_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_skill_option
    ADD CONSTRAINT class_skill_option_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.character_class(class_id) ON DELETE CASCADE;


--
-- Name: class_skill_option class_skill_option_skill_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_skill_option
    ADD CONSTRAINT class_skill_option_skill_id_fkey FOREIGN KEY (skill_id) REFERENCES public.skill(skill_id);


--
-- Name: currency currency_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.currency
    ADD CONSTRAINT currency_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: damage_type damage_type_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_type
    ADD CONSTRAINT damage_type_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: damage_types damage_types_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.damage_types
    ADD CONSTRAINT damage_types_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: enchantment_types enchantment_types_damage_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enchantment_types
    ADD CONSTRAINT enchantment_types_damage_type_id_fkey FOREIGN KEY (damage_type_id) REFERENCES public.damage_types(id);


--
-- Name: equipment_category equipment_category_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_category
    ADD CONSTRAINT equipment_category_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: equipment_item equipment_item_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_item
    ADD CONSTRAINT equipment_item_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.equipment_category(equipment_category_id);


--
-- Name: equipment_item equipment_item_cost_money_value_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_item
    ADD CONSTRAINT equipment_item_cost_money_value_id_fkey FOREIGN KEY (cost_money_value_id) REFERENCES public.money_value(money_value_id);


--
-- Name: equipment_item equipment_item_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_item
    ADD CONSTRAINT equipment_item_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: equipment_item equipment_item_mod_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_item
    ADD CONSTRAINT equipment_item_mod_id_fkey FOREIGN KEY (mod_id) REFERENCES public.mod_package(mod_id);


--
-- Name: equipment_item equipment_item_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_item
    ADD CONSTRAINT equipment_item_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.source_book(source_id);


--
-- Name: equipment_slots equipment_slots_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.equipment_slots
    ADD CONSTRAINT equipment_slots_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: feat feat_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat
    ADD CONSTRAINT feat_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.feat_category(feat_category_id);


--
-- Name: feat feat_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat
    ADD CONSTRAINT feat_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: feat feat_mod_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat
    ADD CONSTRAINT feat_mod_id_fkey FOREIGN KEY (mod_id) REFERENCES public.mod_package(mod_id);


--
-- Name: feat_prerequisite feat_prerequisite_ability_score_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat_prerequisite
    ADD CONSTRAINT feat_prerequisite_ability_score_id_fkey FOREIGN KEY (ability_score_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: feat_prerequisite feat_prerequisite_feat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat_prerequisite
    ADD CONSTRAINT feat_prerequisite_feat_id_fkey FOREIGN KEY (feat_id) REFERENCES public.feat(feat_id) ON DELETE CASCADE;


--
-- Name: feat_section feat_section_feat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat_section
    ADD CONSTRAINT feat_section_feat_id_fkey FOREIGN KEY (feat_id) REFERENCES public.feat(feat_id) ON DELETE CASCADE;


--
-- Name: feat feat_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feat
    ADD CONSTRAINT feat_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.source_book(source_id);


--
-- Name: campaign_npcs fk14pqiuhd74pedl6b728ym4wci; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_npcs
    ADD CONSTRAINT fk14pqiuhd74pedl6b728ym4wci FOREIGN KEY (class_id) REFERENCES public.character_class(class_id);


--
-- Name: quest_rewards fk905wmko6byka0h6kqvlvsoe6p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_rewards
    ADD CONSTRAINT fk905wmko6byka0h6kqvlvsoe6p FOREIGN KEY (currency_type_id) REFERENCES public.currency(currency_id);


--
-- Name: battles fk_battle_campaign; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.battles
    ADD CONSTRAINT fk_battle_campaign FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: battles fk_battle_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.battles
    ADD CONSTRAINT fk_battle_created_by FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: blueprint_homebrew fk_bh_blueprint; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_homebrew
    ADD CONSTRAINT fk_bh_blueprint FOREIGN KEY (blueprint_id) REFERENCES public.campaign_blueprints(id) ON DELETE CASCADE;


--
-- Name: blueprint_homebrew fk_bh_package; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_homebrew
    ADD CONSTRAINT fk_bh_package FOREIGN KEY (package_id) REFERENCES public.homebrew_packages(id);


--
-- Name: blueprint_locations fk_bloc_blueprint; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_locations
    ADD CONSTRAINT fk_bloc_blueprint FOREIGN KEY (blueprint_id) REFERENCES public.campaign_blueprints(id) ON DELETE CASCADE;


--
-- Name: blueprint_locations fk_bloc_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_locations
    ADD CONSTRAINT fk_bloc_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: campaign_blueprints fk_blueprint_author; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_blueprints
    ADD CONSTRAINT fk_blueprint_author FOREIGN KEY (author_id) REFERENCES public.users(id);


--
-- Name: campaign_blueprints fk_blueprint_deleted_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_blueprints
    ADD CONSTRAINT fk_blueprint_deleted_by FOREIGN KEY (deleted_by) REFERENCES public.users(id);


--
-- Name: campaign_blueprints fk_blueprint_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_blueprints
    ADD CONSTRAINT fk_blueprint_parent FOREIGN KEY (parent_id) REFERENCES public.campaign_blueprints(id);


--
-- Name: campaign_blueprints fk_blueprint_universe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_blueprints
    ADD CONSTRAINT fk_blueprint_universe FOREIGN KEY (universe_id) REFERENCES public.universes(id);


--
-- Name: blueprint_npcs fk_bnpc_blueprint; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_npcs
    ADD CONSTRAINT fk_bnpc_blueprint FOREIGN KEY (blueprint_id) REFERENCES public.campaign_blueprints(id) ON DELETE CASCADE;


--
-- Name: blueprint_npcs fk_bnpc_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_npcs
    ADD CONSTRAINT fk_bnpc_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: blueprint_npcs fk_bnpc_monster; Type: FK CONSTRAINT; Schema: public; Owner: -
--



--
-- Name: blueprint_npc_spells fk_bnpcs_npc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_npc_spells
    ADD CONSTRAINT fk_bnpcs_npc FOREIGN KEY (npc_id) REFERENCES public.blueprint_npcs(id) ON DELETE CASCADE;


--
-- Name: blueprint_quest_rewards fk_bqr_item_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_quest_rewards
    ADD CONSTRAINT fk_bqr_item_template FOREIGN KEY (item_template_id) REFERENCES public.item_templates(id);


--
-- Name: blueprint_quest_rewards fk_bqr_quest; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_quest_rewards
    ADD CONSTRAINT fk_bqr_quest FOREIGN KEY (quest_id) REFERENCES public.blueprint_quests(id) ON DELETE CASCADE;


--
-- Name: blueprint_quests fk_bquest_blueprint; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_quests
    ADD CONSTRAINT fk_bquest_blueprint FOREIGN KEY (blueprint_id) REFERENCES public.campaign_blueprints(id) ON DELETE CASCADE;


--
-- Name: blueprint_quests fk_bquest_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_quests
    ADD CONSTRAINT fk_bquest_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: buffs_debuffs fk_buffdebuff_homebrew; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.buffs_debuffs
    ADD CONSTRAINT fk_buffdebuff_homebrew FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id) ON DELETE SET NULL;


--
-- Name: character_active_effects fk_cae_applied_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_active_effects
    ADD CONSTRAINT fk_cae_applied_by FOREIGN KEY (applied_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: character_active_effects fk_cae_buffdebuff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_active_effects
    ADD CONSTRAINT fk_cae_buffdebuff FOREIGN KEY (buff_debuff_id) REFERENCES public.buffs_debuffs(id) ON DELETE RESTRICT;


--
-- Name: character_active_effects fk_cae_character; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_active_effects
    ADD CONSTRAINT fk_cae_character FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: campaign_homebrew fk_camphb_campaign; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_homebrew
    ADD CONSTRAINT fk_camphb_campaign FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: campaign_homebrew fk_camphb_package; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_homebrew
    ADD CONSTRAINT fk_camphb_package FOREIGN KEY (package_id) REFERENCES public.homebrew_packages(id) ON DELETE CASCADE;


--
-- Name: character_class_levels fk_ccl_char; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_class_levels
    ADD CONSTRAINT fk_ccl_char FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: character_class_levels fk_ccl_class_content; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_class_levels
    ADD CONSTRAINT fk_ccl_class_content FOREIGN KEY (class_id) REFERENCES public.character_class(class_id);


--
-- Name: characters fk_char_background_content; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.characters
    ADD CONSTRAINT fk_char_background_content FOREIGN KEY (background_id) REFERENCES public.background(background_id);


--
-- Name: characters fk_char_owner; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.characters
    ADD CONSTRAINT fk_char_owner FOREIGN KEY (owner_id) REFERENCES public.users(id);


--
-- Name: characters fk_character_blueprint; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.characters
    ADD CONSTRAINT fk_character_blueprint FOREIGN KEY (blueprint_id) REFERENCES public.campaign_blueprints(id);


--
-- Name: character_reward_selection fk_character_reward_selection_option_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_reward_selection
    ADD CONSTRAINT fk_character_reward_selection_option_group FOREIGN KEY (reward_option_id, reward_group_id) REFERENCES public.class_level_reward_option(reward_option_id, reward_group_id) ON DELETE CASCADE;


--
-- Name: characters fk_characters_campaign; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.characters
    ADD CONSTRAINT fk_characters_campaign FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: character_resources fk_charres_character; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_resources
    ADD CONSTRAINT fk_charres_character FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: character_resources fk_charres_restype; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_resources
    ADD CONSTRAINT fk_charres_restype FOREIGN KEY (resource_type_id) REFERENCES public.custom_resource_types(id) ON DELETE RESTRICT;


--
-- Name: character_known_spells fk_cks_character; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_known_spells
    ADD CONSTRAINT fk_cks_character FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: character_known_spells fk_cks_spell_content; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_known_spells
    ADD CONSTRAINT fk_cks_spell_content FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id);


--
-- Name: campaign_members fk_cm_campaign; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_members
    ADD CONSTRAINT fk_cm_campaign FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: campaign_members fk_cm_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_members
    ADD CONSTRAINT fk_cm_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: battle_combatants fk_combatant_added_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.battle_combatants
    ADD CONSTRAINT fk_combatant_added_by FOREIGN KEY (added_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: battle_combatants fk_combatant_battle; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.battle_combatants
    ADD CONSTRAINT fk_combatant_battle FOREIGN KEY (battle_id) REFERENCES public.battles(id) ON DELETE CASCADE;


--
-- Name: battle_combatants fk_combatant_character; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.battle_combatants
    ADD CONSTRAINT fk_combatant_character FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: battle_combatants fk_combatant_monster; Type: FK CONSTRAINT; Schema: public; Owner: -
--



--
-- Name: character_skill_proficiencies fk_csp_character; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_skill_proficiencies
    ADD CONSTRAINT fk_csp_character FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: character_skill_proficiencies fk_csp_skill_content; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_skill_proficiencies
    ADD CONSTRAINT fk_csp_skill_content FOREIGN KEY (skill_id) REFERENCES public.skill(skill_id);


--
-- Name: character_stats fk_cstat_ability_content; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_stats
    ADD CONSTRAINT fk_cstat_ability_content FOREIGN KEY (stat_type_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: character_stats fk_cstat_char; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_stats
    ADD CONSTRAINT fk_cstat_char FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: enchantment_types fk_enchtype_buffdebuff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enchantment_types
    ADD CONSTRAINT fk_enchtype_buffdebuff FOREIGN KEY (buff_debuff_id) REFERENCES public.buffs_debuffs(id) ON DELETE SET NULL;


--
-- Name: enchantment_types fk_enchtype_homebrew; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enchantment_types
    ADD CONSTRAINT fk_enchtype_homebrew FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id) ON DELETE SET NULL;


--
-- Name: gm_homebrew_library fk_gmhblib_package; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gm_homebrew_library
    ADD CONSTRAINT fk_gmhblib_package FOREIGN KEY (package_id) REFERENCES public.homebrew_packages(id) ON DELETE CASCADE;


--
-- Name: gm_homebrew_library fk_gmhblib_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gm_homebrew_library
    ADD CONSTRAINT fk_gmhblib_user FOREIGN KEY (gm_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: gm_session_notes fk_gmnotes_author; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gm_session_notes
    ADD CONSTRAINT fk_gmnotes_author FOREIGN KEY (author_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: gm_session_notes fk_gmnotes_campaign; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gm_session_notes
    ADD CONSTRAINT fk_gmnotes_campaign FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: homebrew_ratings fk_hbrating_package; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_ratings
    ADD CONSTRAINT fk_hbrating_package FOREIGN KEY (package_id) REFERENCES public.homebrew_packages(id) ON DELETE CASCADE;


--
-- Name: homebrew_ratings fk_hbrating_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_ratings
    ADD CONSTRAINT fk_hbrating_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: homebrew_content_items fk_hci_package; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_content_items
    ADD CONSTRAINT fk_hci_package FOREIGN KEY (package_id) REFERENCES public.homebrew_packages(id) ON DELETE CASCADE;


--
-- Name: homebrew_content_versions fk_hcv_package; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_content_versions
    ADD CONSTRAINT fk_hcv_package FOREIGN KEY (package_id) REFERENCES public.homebrew_packages(id) ON DELETE CASCADE;


--
-- Name: homebrew_packages fk_homebrew_author; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_packages
    ADD CONSTRAINT fk_homebrew_author FOREIGN KEY (author_id) REFERENCES public.users(id);


--
-- Name: homebrew_packages fk_homebrew_deleted_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_packages
    ADD CONSTRAINT fk_homebrew_deleted_by FOREIGN KEY (deleted_by) REFERENCES public.users(id);


--
-- Name: homebrew_packages fk_homebrew_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_packages
    ADD CONSTRAINT fk_homebrew_parent FOREIGN KEY (parent_id) REFERENCES public.homebrew_packages(id) ON DELETE SET NULL;


--
-- Name: homebrew_package_tags fk_hpt_package; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_package_tags
    ADD CONSTRAINT fk_hpt_package FOREIGN KEY (package_id) REFERENCES public.homebrew_packages(id) ON DELETE CASCADE;


--
-- Name: homebrew_package_tags fk_hpt_tag; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.homebrew_package_tags
    ADD CONSTRAINT fk_hpt_tag FOREIGN KEY (tag_id) REFERENCES public.homebrew_tags(id);


--
-- Name: item_enchantments fk_itemench_enchtype; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_enchantments
    ADD CONSTRAINT fk_itemench_enchtype FOREIGN KEY (enchantment_type_id) REFERENCES public.enchantment_types(id) ON DELETE RESTRICT;


--
-- Name: item_enchantments fk_itemench_instance; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_enchantments
    ADD CONSTRAINT fk_itemench_instance FOREIGN KEY (item_instance_id) REFERENCES public.item_instances(id) ON DELETE CASCADE;


--
-- Name: item_instances fk_iteminst_owner; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_instances
    ADD CONSTRAINT fk_iteminst_owner FOREIGN KEY (owner_character_id) REFERENCES public.characters(id) ON DELETE SET NULL;


--
-- Name: item_instances fk_iteminst_shared_storage; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_instances
    ADD CONSTRAINT fk_iteminst_shared_storage FOREIGN KEY (shared_storage_id) REFERENCES public.shared_storage(id) ON DELETE SET NULL;


--
-- Name: item_instances fk_iteminst_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_instances
    ADD CONSTRAINT fk_iteminst_template FOREIGN KEY (template_id) REFERENCES public.item_templates(id) ON DELETE RESTRICT;


--
-- Name: item_instances fk_iteminst_equipment_item; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_instances
    ADD CONSTRAINT fk_iteminst_equipment_item FOREIGN KEY (equipment_item_id) REFERENCES public.equipment_item(equipment_item_id) ON DELETE RESTRICT;


--
-- Name: item_instances fk_iteminst_magic_item; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_instances
    ADD CONSTRAINT fk_iteminst_magic_item FOREIGN KEY (magic_item_id) REFERENCES public.magic_item(magic_item_id) ON DELETE RESTRICT;


--
-- Name: item_templates fk_itemtpl_homebrew; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_templates
    ADD CONSTRAINT fk_itemtpl_homebrew FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id) ON DELETE SET NULL;


--
-- Name: item_templates fk_itemtpl_item_type; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_templates
    ADD CONSTRAINT fk_itemtpl_item_type FOREIGN KEY (item_type_id) REFERENCES public.item_types(id) ON DELETE SET NULL;


--
-- Name: item_templates fk_itemtpl_skill; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_templates
    ADD CONSTRAINT fk_itemtpl_skill FOREIGN KEY (skill_id) REFERENCES public.skills(id) ON DELETE SET NULL;


--
-- Name: item_template_buffs fk_itemtplbuff_buffdebuff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_template_buffs
    ADD CONSTRAINT fk_itemtplbuff_buffdebuff FOREIGN KEY (buff_debuff_id) REFERENCES public.buffs_debuffs(id) ON DELETE CASCADE;


--
-- Name: item_template_buffs fk_itemtplbuff_template; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_template_buffs
    ADD CONSTRAINT fk_itemtplbuff_template FOREIGN KEY (template_id) REFERENCES public.item_templates(id) ON DELETE CASCADE;


--
-- Name: item_types fk_itemtype_homebrew; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_types
    ADD CONSTRAINT fk_itemtype_homebrew FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id) ON DELETE SET NULL;


--
-- Name: item_types fk_itemtype_skill; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_types
    ADD CONSTRAINT fk_itemtype_skill FOREIGN KEY (skill_id) REFERENCES public.skills(id) ON DELETE SET NULL;


--
-- Name: campaign_locations fk_location_campaign; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_locations
    ADD CONSTRAINT fk_location_campaign FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: campaign_locations fk_location_creator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_locations
    ADD CONSTRAINT fk_location_creator FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: campaign_npcs fk_npc_campaign; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_npcs
    ADD CONSTRAINT fk_npc_campaign FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: campaign_npcs fk_npc_creator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_npcs
    ADD CONSTRAINT fk_npc_creator FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: npc_notes fk_npcnote_author; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.npc_notes
    ADD CONSTRAINT fk_npcnote_author FOREIGN KEY (author_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: npc_notes fk_npcnote_npc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.npc_notes
    ADD CONSTRAINT fk_npcnote_npc FOREIGN KEY (npc_id) REFERENCES public.campaign_npcs(id) ON DELETE CASCADE;


--
-- Name: campaign_quests fk_quest_campaign; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_quests
    ADD CONSTRAINT fk_quest_campaign FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: campaign_quests fk_quest_creator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_quests
    ADD CONSTRAINT fk_quest_creator FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: quest_items fk_questitem_itemtpl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_items
    ADD CONSTRAINT fk_questitem_itemtpl FOREIGN KEY (item_template_id) REFERENCES public.item_templates(id) ON DELETE CASCADE;


--
-- Name: quest_items fk_questitem_quest; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_items
    ADD CONSTRAINT fk_questitem_quest FOREIGN KEY (quest_id) REFERENCES public.campaign_quests(id) ON DELETE CASCADE;


--
-- Name: quest_locations fk_questloc_location; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_locations
    ADD CONSTRAINT fk_questloc_location FOREIGN KEY (location_id) REFERENCES public.campaign_locations(id) ON DELETE CASCADE;


--
-- Name: quest_locations fk_questloc_quest; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_locations
    ADD CONSTRAINT fk_questloc_quest FOREIGN KEY (quest_id) REFERENCES public.campaign_quests(id) ON DELETE CASCADE;


--
-- Name: quest_notes fk_questnote_author; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_notes
    ADD CONSTRAINT fk_questnote_author FOREIGN KEY (author_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: quest_notes fk_questnote_quest; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_notes
    ADD CONSTRAINT fk_questnote_quest FOREIGN KEY (quest_id) REFERENCES public.campaign_quests(id) ON DELETE CASCADE;


--
-- Name: quest_npcs fk_questnpc_npc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_npcs
    ADD CONSTRAINT fk_questnpc_npc FOREIGN KEY (npc_id) REFERENCES public.campaign_npcs(id) ON DELETE CASCADE;


--
-- Name: quest_npcs fk_questnpc_quest; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_npcs
    ADD CONSTRAINT fk_questnpc_quest FOREIGN KEY (quest_id) REFERENCES public.campaign_quests(id) ON DELETE CASCADE;


--
-- Name: quest_rewards fk_questreward_itemtpl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_rewards
    ADD CONSTRAINT fk_questreward_itemtpl FOREIGN KEY (item_template_id) REFERENCES public.item_templates(id) ON DELETE SET NULL;


--
-- Name: quest_rewards fk_questreward_quest; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quest_rewards
    ADD CONSTRAINT fk_questreward_quest FOREIGN KEY (quest_id) REFERENCES public.campaign_quests(id) ON DELETE CASCADE;


--
-- Name: custom_resource_types fk_restype_homebrew; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_resource_types
    ADD CONSTRAINT fk_restype_homebrew FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id) ON DELETE SET NULL;


--
-- Name: shared_storage fk_shared_storage_campaign; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shared_storage
    ADD CONSTRAINT fk_shared_storage_campaign FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: shared_storage fk_shared_storage_creator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shared_storage
    ADD CONSTRAINT fk_shared_storage_creator FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: skills fk_skill_homebrew; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT fk_skill_homebrew FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id) ON DELETE SET NULL;


--
-- Name: skill_effects fk_skilleffect_buffdebuff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skill_effects
    ADD CONSTRAINT fk_skilleffect_buffdebuff FOREIGN KEY (buff_debuff_id) REFERENCES public.buffs_debuffs(id) ON DELETE CASCADE;


--
-- Name: skill_effects fk_skilleffect_skill; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skill_effects
    ADD CONSTRAINT fk_skilleffect_skill FOREIGN KEY (skill_id) REFERENCES public.skills(id) ON DELETE CASCADE;


--
-- Name: universes fk_universe_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.universes
    ADD CONSTRAINT fk_universe_created_by FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: character_wallets fk_wallet_character; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_wallets
    ADD CONSTRAINT fk_wallet_character FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: character_wallets fk_wallet_currency_content; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_wallets
    ADD CONSTRAINT fk_wallet_currency_content FOREIGN KEY (currency_type_id) REFERENCES public.currency(currency_id);


--
-- Name: wallet_transactions fk_wallet_tx_character; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wallet_transactions
    ADD CONSTRAINT fk_wallet_tx_character FOREIGN KEY (character_id) REFERENCES public.characters(id) ON DELETE CASCADE;


--
-- Name: wallet_transactions fk_wallet_tx_currency_content; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wallet_transactions
    ADD CONSTRAINT fk_wallet_tx_currency_content FOREIGN KEY (currency_type_id) REFERENCES public.currency(currency_id);


--
-- Name: blueprint_npc_spells fkbmmv2klon7ipxo7qqayargylc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_npc_spells
    ADD CONSTRAINT fkbmmv2klon7ipxo7qqayargylc FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id);


--
-- Name: campaign_npc_spells fkd8f4wg36amd8fk6gqm7uco7b6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campaign_npc_spells
    ADD CONSTRAINT fkd8f4wg36amd8fk6gqm7uco7b6 FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id);


--
-- Name: character_known_spells fkdw5i96fg6bc197ru9dre0aweg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.character_known_spells
    ADD CONSTRAINT fkdw5i96fg6bc197ru9dre0aweg FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id);


--
-- Name: blueprint_quest_rewards fkk6wj0s0cqwim4nuq6lbo0u8wd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_quest_rewards
    ADD CONSTRAINT fkk6wj0s0cqwim4nuq6lbo0u8wd FOREIGN KEY (currency_type_id) REFERENCES public.currency(currency_id);


--
-- Name: blueprint_npcs fklmwyxn254xqvshqr2j1v5v2lf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blueprint_npcs
    ADD CONSTRAINT fklmwyxn254xqvshqr2j1v5v2lf FOREIGN KEY (class_id) REFERENCES public.character_class(class_id);


--
-- Name: item_instances item_instances_slot_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_instances
    ADD CONSTRAINT item_instances_slot_id_fkey FOREIGN KEY (slot_id) REFERENCES public.equipment_slots(id);


--
-- Name: item_rarities item_rarities_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_rarities
    ADD CONSTRAINT item_rarities_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: item_template_buffs item_template_buffs_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_template_buffs
    ADD CONSTRAINT item_template_buffs_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: item_templates item_templates_damage_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_templates
    ADD CONSTRAINT item_templates_damage_type_id_fkey FOREIGN KEY (damage_type_id) REFERENCES public.damage_types(id);


--
-- Name: item_templates item_templates_rarity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_templates
    ADD CONSTRAINT item_templates_rarity_id_fkey FOREIGN KEY (rarity_id) REFERENCES public.item_rarities(id);


--
-- Name: item_types item_types_damage_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_types
    ADD CONSTRAINT item_types_damage_type_id_fkey FOREIGN KEY (damage_type_id) REFERENCES public.damage_types(id);


--
-- Name: item_types item_types_slot_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_types
    ADD CONSTRAINT item_types_slot_id_fkey FOREIGN KEY (slot_id) REFERENCES public.equipment_slots(id);


--
-- Name: magic_item_allowed_equipment magic_item_allowed_equipment_equipment_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item_allowed_equipment
    ADD CONSTRAINT magic_item_allowed_equipment_equipment_item_id_fkey FOREIGN KEY (equipment_item_id) REFERENCES public.equipment_item(equipment_item_id);


--
-- Name: magic_item_allowed_equipment magic_item_allowed_equipment_magic_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item_allowed_equipment
    ADD CONSTRAINT magic_item_allowed_equipment_magic_item_id_fkey FOREIGN KEY (magic_item_id) REFERENCES public.magic_item(magic_item_id) ON DELETE CASCADE;


--
-- Name: magic_item magic_item_cost_money_value_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item
    ADD CONSTRAINT magic_item_cost_money_value_id_fkey FOREIGN KEY (cost_money_value_id) REFERENCES public.money_value(money_value_id);


--
-- Name: magic_item magic_item_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item
    ADD CONSTRAINT magic_item_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: magic_item magic_item_magic_item_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item
    ADD CONSTRAINT magic_item_magic_item_type_id_fkey FOREIGN KEY (magic_item_type_id) REFERENCES public.magic_item_type(magic_item_type_id);


--
-- Name: magic_item magic_item_mod_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item
    ADD CONSTRAINT magic_item_mod_id_fkey FOREIGN KEY (mod_id) REFERENCES public.mod_package(mod_id);


--
-- Name: magic_item_rarity magic_item_rarity_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item_rarity
    ADD CONSTRAINT magic_item_rarity_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: magic_item magic_item_rarity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item
    ADD CONSTRAINT magic_item_rarity_id_fkey FOREIGN KEY (rarity_id) REFERENCES public.magic_item_rarity(magic_item_rarity_id);


--
-- Name: magic_item magic_item_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item
    ADD CONSTRAINT magic_item_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.source_book(source_id);


--
-- Name: magic_item_type magic_item_type_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.magic_item_type
    ADD CONSTRAINT magic_item_type_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: money_value money_value_currency_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.money_value
    ADD CONSTRAINT money_value_currency_id_fkey FOREIGN KEY (currency_id) REFERENCES public.currency(currency_id);


--
-- Name: proficiency_skills proficiency_skills_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.proficiency_skills
    ADD CONSTRAINT proficiency_skills_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: random_table_entry random_table_entry_linked_equipment_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.random_table_entry
    ADD CONSTRAINT random_table_entry_linked_equipment_item_id_fkey FOREIGN KEY (linked_equipment_item_id) REFERENCES public.equipment_item(equipment_item_id);


--
-- Name: random_table_entry random_table_entry_linked_magic_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.random_table_entry
    ADD CONSTRAINT random_table_entry_linked_magic_item_id_fkey FOREIGN KEY (linked_magic_item_id) REFERENCES public.magic_item(magic_item_id);


--
-- Name: random_table_entry random_table_entry_random_table_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.random_table_entry
    ADD CONSTRAINT random_table_entry_random_table_id_fkey FOREIGN KEY (random_table_id) REFERENCES public.random_table(random_table_id) ON DELETE CASCADE;


--
-- Name: random_table random_table_mod_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.random_table
    ADD CONSTRAINT random_table_mod_id_fkey FOREIGN KEY (mod_id) REFERENCES public.mod_package(mod_id);


--
-- Name: random_table random_table_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.random_table
    ADD CONSTRAINT random_table_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.source_book(source_id);


--
-- Name: skill skill_ability_score_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skill
    ADD CONSTRAINT skill_ability_score_id_fkey FOREIGN KEY (ability_score_id) REFERENCES public.ability_score(ability_score_id);


--
-- Name: skill_effects skill_effects_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skill_effects
    ADD CONSTRAINT skill_effects_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: skill skill_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skill
    ADD CONSTRAINT skill_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: skills skills_damage_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skills
    ADD CONSTRAINT skills_damage_type_id_fkey FOREIGN KEY (damage_type_id) REFERENCES public.damage_types(id);


--
-- Name: species species_creature_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species
    ADD CONSTRAINT species_creature_type_id_fkey FOREIGN KEY (creature_type_id) REFERENCES public.creature_type(creature_type_id);


--
-- Name: species species_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species
    ADD CONSTRAINT species_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: species species_mod_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species
    ADD CONSTRAINT species_mod_id_fkey FOREIGN KEY (mod_id) REFERENCES public.mod_package(mod_id);


--
-- Name: species_size_option species_size_option_character_size_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_size_option
    ADD CONSTRAINT species_size_option_character_size_id_fkey FOREIGN KEY (character_size_id) REFERENCES public.character_size(character_size_id);


--
-- Name: species_size_option species_size_option_species_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_size_option
    ADD CONSTRAINT species_size_option_species_id_fkey FOREIGN KEY (species_id) REFERENCES public.species(species_id) ON DELETE CASCADE;


--
-- Name: species species_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species
    ADD CONSTRAINT species_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.source_book(source_id);


--
-- Name: species_speed species_speed_species_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_speed
    ADD CONSTRAINT species_speed_species_id_fkey FOREIGN KEY (species_id) REFERENCES public.species(species_id) ON DELETE CASCADE;


--
-- Name: species_trait_effect species_trait_effect_damage_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_trait_effect
    ADD CONSTRAINT species_trait_effect_damage_type_id_fkey FOREIGN KEY (damage_type_id) REFERENCES public.damage_type(damage_type_id);


--
-- Name: species_trait_effect species_trait_effect_species_trait_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_trait_effect
    ADD CONSTRAINT species_trait_effect_species_trait_id_fkey FOREIGN KEY (species_trait_id) REFERENCES public.species_trait(species_trait_id) ON DELETE CASCADE;


--
-- Name: species_trait_effect species_trait_effect_spell_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_trait_effect
    ADD CONSTRAINT species_trait_effect_spell_id_fkey FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id);


--
-- Name: species_trait species_trait_species_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.species_trait
    ADD CONSTRAINT species_trait_species_id_fkey FOREIGN KEY (species_id) REFERENCES public.species(species_id) ON DELETE CASCADE;


--
-- Name: spell_class spell_class_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_class
    ADD CONSTRAINT spell_class_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.character_class(class_id) ON DELETE CASCADE;


--
-- Name: spell_class spell_class_spell_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_class
    ADD CONSTRAINT spell_class_spell_id_fkey FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id) ON DELETE CASCADE;


--
-- Name: spell_component spell_component_cost_money_value_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_component
    ADD CONSTRAINT spell_component_cost_money_value_id_fkey FOREIGN KEY (cost_money_value_id) REFERENCES public.money_value(money_value_id);


--
-- Name: spell_component spell_component_spell_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_component
    ADD CONSTRAINT spell_component_spell_id_fkey FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id) ON DELETE CASCADE;


--
-- Name: spell spell_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell
    ADD CONSTRAINT spell_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: spell spell_mod_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell
    ADD CONSTRAINT spell_mod_id_fkey FOREIGN KEY (mod_id) REFERENCES public.mod_package(mod_id);


--
-- Name: spell spell_school_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell
    ADD CONSTRAINT spell_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.spell_school(spell_school_id);


--
-- Name: spell_scroll_crafting_rule spell_scroll_crafting_rule_cost_money_value_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_scroll_crafting_rule
    ADD CONSTRAINT spell_scroll_crafting_rule_cost_money_value_id_fkey FOREIGN KEY (cost_money_value_id) REFERENCES public.money_value(money_value_id);


--
-- Name: spell spell_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell
    ADD CONSTRAINT spell_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.source_book(source_id);


--
-- Name: spell_subclass spell_subclass_spell_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_subclass
    ADD CONSTRAINT spell_subclass_spell_id_fkey FOREIGN KEY (spell_id) REFERENCES public.spell(spell_id) ON DELETE CASCADE;


--
-- Name: spell_subclass spell_subclass_subclass_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spell_subclass
    ADD CONSTRAINT spell_subclass_subclass_id_fkey FOREIGN KEY (subclass_id) REFERENCES public.subclass(subclass_id) ON DELETE CASCADE;


--
-- Name: subclass subclass_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subclass
    ADD CONSTRAINT subclass_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.character_class(class_id) ON DELETE CASCADE;


--
-- Name: subclass subclass_homebrew_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subclass
    ADD CONSTRAINT subclass_homebrew_id_fkey FOREIGN KEY (homebrew_id) REFERENCES public.homebrew_packages(id);


--
-- Name: weapon_item_property weapon_item_property_ammunition_equipment_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_item_property
    ADD CONSTRAINT weapon_item_property_ammunition_equipment_item_id_fkey FOREIGN KEY (ammunition_equipment_item_id) REFERENCES public.equipment_item(equipment_item_id);


--
-- Name: weapon_item_property weapon_item_property_equipment_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_item_property
    ADD CONSTRAINT weapon_item_property_equipment_item_id_fkey FOREIGN KEY (equipment_item_id) REFERENCES public.equipment_item(equipment_item_id) ON DELETE CASCADE;


--
-- Name: weapon_item_property weapon_item_property_versatile_dice_formula_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_item_property
    ADD CONSTRAINT weapon_item_property_versatile_dice_formula_id_fkey FOREIGN KEY (versatile_dice_formula_id) REFERENCES public.dice_formula(dice_formula_id);


--
-- Name: weapon_item_property weapon_item_property_weapon_property_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_item_property
    ADD CONSTRAINT weapon_item_property_weapon_property_id_fkey FOREIGN KEY (weapon_property_id) REFERENCES public.weapon_property(weapon_property_id);


--
-- Name: weapon_stat weapon_stat_damage_dice_formula_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_stat
    ADD CONSTRAINT weapon_stat_damage_dice_formula_id_fkey FOREIGN KEY (damage_dice_formula_id) REFERENCES public.dice_formula(dice_formula_id);


--
-- Name: weapon_stat weapon_stat_damage_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_stat
    ADD CONSTRAINT weapon_stat_damage_type_id_fkey FOREIGN KEY (damage_type_id) REFERENCES public.damage_type(damage_type_id);


--
-- Name: weapon_stat weapon_stat_equipment_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_stat
    ADD CONSTRAINT weapon_stat_equipment_item_id_fkey FOREIGN KEY (equipment_item_id) REFERENCES public.equipment_item(equipment_item_id) ON DELETE CASCADE;


--
-- Name: weapon_stat weapon_stat_mastery_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weapon_stat
    ADD CONSTRAINT weapon_stat_mastery_id_fkey FOREIGN KEY (mastery_id) REFERENCES public.weapon_mastery(weapon_mastery_id);


--
-- PostgreSQL database dump complete
--


