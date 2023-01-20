package de.hsrm.mi.swt02.backend.api.map.dto;

import de.hsrm.mi.swt02.backend.domain.map.GameAsset;

public record GameAssetDTO(
        long assetId,
        int objectTypeId,
        double x,
        double y,
        int rotation,
        String texture
) {

    public static GameAssetDTO from (GameAsset asset) {
        return new GameAssetDTO(
                asset.getId(),
                asset.getObjectTypeId(),
                asset.getX(),
                asset.getY(),
                asset.getRotation(),
                asset.getTexture()
        );
    }
}