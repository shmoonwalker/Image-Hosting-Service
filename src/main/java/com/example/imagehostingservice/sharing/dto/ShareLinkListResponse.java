package com.example.imagehostingservice.sharing.dto;

import java.util.List;

public record ShareLinkListResponse(
        List<ShareLinkResponse> shareLinks
) {
}
