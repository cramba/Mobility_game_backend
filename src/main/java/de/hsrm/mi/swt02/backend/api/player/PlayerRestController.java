package de.hsrm.mi.swt02.backend.api.player;

import de.hsrm.mi.swt02.backend.api.player.dtos.AddPlayerRequestDTO;
import de.hsrm.mi.swt02.backend.api.player.dtos.GetPlayerResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/player")
public class PlayerRestController {

    @Autowired
    PlayerServiceImpl playerService;

    @Operation(summary = "Get all registered users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found users")
    })
    @GetMapping("")
    public List<GetPlayerResponseDTO> getAllPlayers() {
        List<GetPlayerResponseDTO> uDTOs = new ArrayList<GetPlayerResponseDTO>(
                playerService.findAllPlayers()
                        .stream()
                        .map(GetPlayerResponseDTO::from)
                        .toList());
        return uDTOs;
    }

    @Operation(summary = "Posting a new user to the DB")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User was created"),
            @ApiResponse(responseCode = "400", description = "User JSON wrong syntax")
    })
    @PostMapping("")
    public long postNewPlayer(
            @Schema(description = "User name", defaultValue = "Josef Weitz")
            @RequestBody AddPlayerRequestDTO uDTO) {
        Player u = playerService.createPlayer(uDTO.userName());

        return u.getId();
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User was created"),
            @ApiResponse(responseCode = "500", description = "User was not found and threw Exception internally")
    })
    @GetMapping("/{id}")
    public GetPlayerResponseDTO getPlayer(
            @Schema(description = "User ID", defaultValue = "1")
            @PathVariable("id") long id) {
        Player player = playerService.findPlayerById(id);
        return GetPlayerResponseDTO.from(player);
    }

    @Operation(summary = "Delete user by given ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User was deleted"),
            @ApiResponse(responseCode = "500", description = "User was not found and threw Exception internally")
    })
    @DeleteMapping("/{id}")
    public void delPlayer(
            @Schema(description = "User ID", defaultValue = "1")
            @PathVariable("id") long id) {

        playerService.deletePlayer(id);
    }
}
