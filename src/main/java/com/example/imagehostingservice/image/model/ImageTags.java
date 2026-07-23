package com.example.imagehostingservice.image.model;

import java.util.List;

public record ImageTags(
        List<String> objects,
        List<String> tags,
        List<String> colors
) {
}
