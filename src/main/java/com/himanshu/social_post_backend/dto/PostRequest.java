package com.himanshu.social_post_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PostRequest {

    @NotBlank(message = "Post text must not be blank")
    @Size(max = 3000, message = "Post text must not exceed 3000 characters")
    private String text;

    @NotEmpty(message = "At least one platform must be selected")
    private List<String> platformIds;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<String> getPlatformIds() { return platformIds; }
    public void setPlatformIds(List<String> platformIds) { this.platformIds = platformIds; }
}