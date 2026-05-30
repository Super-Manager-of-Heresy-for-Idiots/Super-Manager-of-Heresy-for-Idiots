package com.dnd.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGmNoteRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String content;
}
