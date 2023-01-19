package de.hsrm.mi.swt02.backend.websocket.api.npc;

import de.hsrm.mi.swt02.backend.npc.NpcInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import de.hsrm.mi.swt02.backend.api.map.service.MapService;
import de.hsrm.mi.swt02.backend.npc.NpcInfo;
import de.hsrm.mi.swt02.backend.npc.NpcInfoResponseDTO;
import de.hsrm.mi.swt02.backend.websocket.model.npc.NpcMessage;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class NpcController {

    @Autowired
    private MapService mapService;

    
    @MessageMapping("/npc.updatepos")
    @SendTo("/topic/npc")
    public NpcMessage updatePos(NpcMessage npcMessage) {
        NpcMessage npcMessageResponse = new NpcMessage();
        log.info("update Position NpcID: " + String.valueOf(npcMessage.npcContent.npcId()));
      
        //mapid still hardcoded
        NpcInfo info = mapService.initNpc(3, npcMessage.npcContent.npcId(), npcMessage.npcContent.currentMapObject().getX(), npcMessage.npcContent.currentMapObject().getY(), npcMessage.npcContent.npcRotation());
        NpcInfoResponseDTO infoDTO = NpcInfoResponseDTO.from(info);
        npcMessageResponse.setNextMapEleInfo(infoDTO);
        // npcMessageResponse.setNextMapEleInfo(NpcInfoResponseDTO.from(info));
        npcMessageResponse.setType(NpcMessage.MessageType.NEW_POSITION_RECEIVED);
        npcMessageResponse.setNpcContent(new NpcInfoDTO(npcMessage.npcContent.npcId(), -1, null, null));
        return npcMessageResponse;
    }
}