package com.dnd.app.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginPageStatsResponse {
    long campaignCount;
    long userCount;
    long vigilDays;
}
