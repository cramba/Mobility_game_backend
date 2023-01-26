package de.hsrm.mi.swt02.backend.api.map.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import de.hsrm.mi.swt02.backend.api.map.dto.GameAssetDTO;
import de.hsrm.mi.swt02.backend.api.map.dto.AddMapObjectsRequestDTO;
import de.hsrm.mi.swt02.backend.api.map.repository.GameAssetRepository;
import de.hsrm.mi.swt02.backend.api.map.repository.MapObjectRepository;
import de.hsrm.mi.swt02.backend.domain.map.GameAsset;
import de.hsrm.mi.swt02.backend.domain.map.Map;
import de.hsrm.mi.swt02.backend.domain.map.MapObject;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.hsrm.mi.swt02.backend.api.map.dto.AddMapObjectRequestDTO;

/**
 * Service Class to handle CRUD operations on the MapObjectRepository.
 */

@Service
@Slf4j
public class MapObjectServiceImpl implements MapObjectService {

    @Autowired
    private MapObjectRepository mapObjRepo;

    @Autowired
    private MapService mapService;

    @Autowired
    private GameAssetRepository gameAssetRepo;

    private int gridSizeX = 300;
    private int gridSizeY = 200;
    private int fieldSize = 10;

    /**
     * @return list containing all MapObjects of repository.
     */
    @Override
    @Transactional
    public List<MapObject> findAllMapObjects() {

        Optional<List<MapObject>> allMaps = Optional.of(mapObjRepo.findAll());

        if (allMaps.isEmpty()) {
            // logger
        }

        return allMaps.get();
    }

    /**
     * gets single MapObject by the handed id
     *
     * @param id id to look for the correct object
     * @return returns MapObject if found
     */
    @Override
    @Transactional
    public MapObject getMapObjectById(long id) {
        Optional<MapObject> foundMapObj = mapObjRepo.findById(id);

        return foundMapObj.orElse(null);
    }

    /**
     * deletes single MapObject by the given id and delete MapObject from
     * MapObjectList in Map
     *
     * @param id id of the MapObject to be deleted.
     */
    @Override
    @Transactional
    public void deleteMapObjectById(long id) {
        this.getMapObjectById(id).getMap().getMapObjects().remove(this.getMapObjectById(id));
        mapObjRepo.deleteById(id);
    }

    /**
     * adds incoming MapObjects from frontend to the repository.
     *
     * @param mapObjects initial converting from JSON to regular java object from
     *                   incoming Request Body in corresponding REST Controller,
     *                   every Entity is saved individually.
     * @param mapId      id to add the MapObjects to the right Map (in MapObjects
     *                   list) and add Map field to the right MapObject
     * @return returns the id of the Entity that was saved last.
     */
    @Override
    @Transactional
    public Long createMapObject(AddMapObjectsRequestDTO mapObjects, long mapId) {

        Map foundMap = mapService.getMapById(mapId);

        this.deleteAllMapObjectsFromMapById(mapId);

        long returnValue = 0L;

        if (!mapObjects.mapObjects().isEmpty()) {
            for (AddMapObjectRequestDTO ele : mapObjects.mapObjects()) {
                MapObject nMapObj = new MapObject(ele.objectTypeId(), ele.x(), ele.y(), ele.rotation());
                nMapObj.setCenterX3d(ele.y());
                nMapObj.setCenterZ3d(ele.x());
                nMapObj.setMap(mapService.getMapById(mapId));
                foundMap.getMapObjects().add(nMapObj);
                returnValue = mapObjRepo.save(nMapObj).getId();
            }
        }

        mapService.saveEditedMap(foundMap);

        return returnValue;
    }

    @Override
    @Transactional
    public void deleteAllMapObjectsFromMapById(long id) {
        Map foundMap = mapService.getMapById(id);

        for (MapObject ele : foundMap.getMapObjects()) {
            mapObjRepo.deleteById(ele.getId());
        }
    }


    /**
     * Add a new MapObjekt to the right Map and save in database that comes from Message Broker.
     * If MapObject has a field that already exist, it will be deleted.
     *
     * @param mapObjectDTO - came from Broker (create) channel
     */

    @Override
    @Transactional
    public void addNewMapObjectFromBroker(AddMapObjectRequestDTO mapObjectDTO, long mapId) {
        Map map = mapService.getMapById(mapId);
        List<MapObject> mapObjectList = map.getMapObjects();
        this.findMapObjectByXandY(mapObjectList, mapObjectDTO)
            .ifPresent(mapObject -> mapObjRepo.delete(mapObject));
        MapObject mapObject = new MapObject(mapObjectDTO.objectTypeId(), mapObjectDTO.x(), mapObjectDTO.y(), mapObjectDTO.rotation());

        mapObject.setCenterX3d(calcMapEleCenterX(mapObjectDTO.y()));
        mapObject.setCenterZ3d(calcMapEleCenterZ(mapObjectDTO.x()));


        mapObjectList.add(mapObject);
        mapObject.setMap(map);
        mapObject.setGameAssets(new ArrayList<GameAsset>());
        this.addNewGameAssetToMapObject(mapObjectDTO.game_assets(), mapObject);
        mapObjRepo.save(mapObject);
    }

    /**
     * Delete a MapObject that comes from Message Broker.
     *
     * @param mapObjectDTO - came from Broker (delete) channel
     */
    @Override
    @Transactional
    public void deleteMapObjectFromBroker(AddMapObjectRequestDTO mapObjectDTO, long mapId) {
        Map map = mapService.getMapById(mapId);
        List<MapObject> mapObjectList = map.getMapObjects();
        this.findMapObjectByXandY(mapObjectList, mapObjectDTO)
                .ifPresent(mapObject -> mapObjRepo.delete(mapObject));
    }

    /**
     * Update a new MapObjekt that comes from Message Broker (rotation).
     *
     * @param mapObjectDTO - came from Broker (update) channel
     */
    @Transactional
    public void updateMapObjectFromBroker(AddMapObjectRequestDTO mapObjectDTO, long mapId) {
        Map map = mapService.getMapById(mapId);
        List<MapObject> mapObjectList = map.getMapObjects();
        this.findMapObjectByXandY(mapObjectList, mapObjectDTO)
                .ifPresent(mapObject -> {
                    mapObject.setRotation(mapObjectDTO.rotation());
                    deleteOldGameAssetsFromMapObject(mapObject);
                    if (!mapObjectDTO.game_assets().isEmpty()) {
                        this.addNewGameAssetToMapObject(mapObjectDTO.game_assets(), mapObject);
                    }
                    mapObjRepo.save(mapObject);
                });
    }

    private void addNewGameAssetToMapObject(List<GameAssetDTO> gameAssetDTOs, MapObject mapObject) {
        gameAssetDTOs.forEach(ele -> {
            GameAsset gameAsset = new GameAsset(ele.objectTypeId(), ele.x(), ele.y(), ele.rotation(), ele.texture(), ele.userId());
            
            gameAsset.setX3d(calcPixelPosNpcX(mapObject.getCenterX3d(),gameAsset.getX()));
            gameAsset.setZ3d(calcPixelPosNpcX(mapObject.getCenterZ3d(),gameAsset.getY()));


            mapObject.getGameAssets().add(gameAsset);
            gameAsset.setMapObject(mapObject);
            gameAssetRepo.save(gameAsset);
        });
    }

    private void deleteOldGameAssetsFromMapObject(MapObject mapObject) {
        mapObject.getGameAssets().forEach(ele -> {
            ele.setMapObject(null);
            gameAssetRepo.delete(ele);
        });
        mapObject.getGameAssets().clear();
    }

    /**
     * @param id Map ID
     * @return All MapObjects from Map
     */
    @Override
    public List<MapObject> getAllMapObjectsFromMap(long id) {
        return mapService.getMapById(id).getMapObjects();
    }
    private Optional<MapObject> findMapObjectByXandY(List<MapObject> mapObjectList, AddMapObjectRequestDTO mapObjectDTO) {
        return mapObjectList.stream()
                .filter(c -> c.getX() == mapObjectDTO.x() && c.getY() == mapObjectDTO.y())
                .findFirst();
    }


    //center 3D xcoord
    public int calcMapEleCenterX(int curMapObjY){
        int curMapObjcenterX = ((int) (this.gridSizeX * -0.5 + curMapObjY * this.fieldSize + this.fieldSize / 2));
        return curMapObjcenterX;
    }

    //center 3D zcoord
    public int calcMapEleCenterZ(int curMapObjY){
        int curMapObjcenterZ = ((int) (this.gridSizeY * -0.5 + curMapObjY * this.fieldSize + this.fieldSize / 2));
        return curMapObjcenterZ;
    }

    // x pixelpos asset 3d
    public double calcPixelPosNpcX(int curMapObjcenterX, double gameAssetX){
        double originX = curMapObjcenterX - this.fieldSize / 2;
        double npcPosX = originX + gameAssetX * this.fieldSize;

        return npcPosX;
    }

    //z pixelpos asset 3d
    public double calcPixelPosNpcZ(int curMapObjcenterZ, double gameAssetZ){
       

        double originZ = curMapObjcenterZ - this.fieldSize / 2;
        double npcPosZ = originZ + gameAssetZ * this.fieldSize;

        return npcPosZ;
    }
}
