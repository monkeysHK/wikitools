package org.hsw.wikitools.feature.get_skull_id.app;

import java.util.Optional;

public class GetSkullIdHandler {
    private final FindHoveredSkullItem findHoveredSkullItem;
    private final FindFacingEntitySkull findFacingEntitySkull;
    private final FindFacingBlockSkull findFacingBlockSkull;

    public GetSkullIdHandler(FindHoveredSkullItem findHoveredSkullItem, FindFacingEntitySkull findFacingEntitySkull, FindFacingBlockSkull findFacingBlockSkull) {
        this.findHoveredSkullItem = findHoveredSkullItem;
        this.findFacingEntitySkull = findFacingEntitySkull;
        this.findFacingBlockSkull = findFacingBlockSkull;
    }

    public Optional<GetSkullIdResponse> getSkullId(GetSkullIdRequest request) {
        Optional<Skull> hoveredSkullItem = findHoveredSkullItem.getHoveredSkull();
        if (hoveredSkullItem.isPresent()) {
            return GetSkullIdResponse.of(hoveredSkullItem);
        }
        Optional<Skull> facingEntitySkull = findFacingEntitySkull.getFacingSkull();
        if (facingEntitySkull.isPresent()) {
            return GetSkullIdResponse.of(facingEntitySkull);
        }
        Optional<Skull> facingBlockSkull = findFacingBlockSkull.getFacingSkull();
        return GetSkullIdResponse.of(facingBlockSkull);
    }

    public static class GetSkullIdRequest {}

    public static class GetSkullIdResponse {
        public final String textureId;

        private GetSkullIdResponse(String textureId) {
            this.textureId = textureId;
        }

        public static Optional<GetSkullIdResponse> of(Optional<Skull> skull) {
            return skull.map(value -> new GetSkullIdResponse(value.textureId));
        }
    }
}
