package com.thesis.social.friend.controller;

import com.thesis.social.friend.dto.BlockResponseDto;
import com.thesis.social.friend.dto.FriendDto;
import com.thesis.social.friend.dto.FriendRequestDirection;
import com.thesis.social.friend.dto.FriendRequestResponseDto;
import com.thesis.social.friend.dto.SendFriendRequestDto;
import com.thesis.social.friend.service.FriendService;
import com.thesis.social.security.AuthenticatedProfile;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @accessGuard.isNotCurrentProfile(#request.receiverId())")
    public FriendRequestResponseDto sendRequest(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @Valid @RequestBody SendFriendRequestDto request
    ) {
        return friendService.sendRequest(profile.getProfileId(), request.receiverId());
    }

    @DeleteMapping("/requests/{requestId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @accessGuard.isCurrentProfileFriendRequestParticipant(#requestId)")
    public FriendRequestResponseDto cancelRequest(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @PathVariable UUID requestId
    ) {
        return friendService.cancelRequest(profile.getProfileId(), requestId);
    }

    @PostMapping("/requests/{requestId}/accept")
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @accessGuard.isCurrentProfileFriendRequestParticipant(#requestId)")
    public FriendRequestResponseDto acceptRequest(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @PathVariable UUID requestId
    ) {
        return friendService.acceptRequest(profile.getProfileId(), requestId);
    }

    @PostMapping("/requests/{requestId}/reject")
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @accessGuard.isCurrentProfileFriendRequestParticipant(#requestId)")
    public FriendRequestResponseDto rejectRequest(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @PathVariable UUID requestId
    ) {
        return friendService.rejectRequest(profile.getProfileId(), requestId);
    }

    @DeleteMapping("/{profileId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @accessGuard.isNotCurrentProfile(#profileId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfriend(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @PathVariable UUID profileId
    ) {
        friendService.unfriend(profile.getProfileId(), profileId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<FriendDto> listFriends(@AuthenticationPrincipal AuthenticatedProfile profile) {
        return friendService.listFriends(profile.getProfileId());
    }

    @GetMapping("/requests/incoming")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<FriendRequestResponseDto> listIncomingRequests(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return friendService.listIncomingRequests(profile.getProfileId(), page, size);
    }

    @GetMapping("/requests/outgoing")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<FriendRequestResponseDto> listOutgoingRequests(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return friendService.listOutgoingRequests(profile.getProfileId(), page, size);
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<FriendRequestResponseDto> listRequests(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @RequestParam FriendRequestDirection direction,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return friendService.listRequests(profile.getProfileId(), direction, page, size);
    }

    @PostMapping("/blocks/{profileId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @accessGuard.isNotCurrentProfile(#profileId)")
    public BlockResponseDto block(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @PathVariable UUID profileId
    ) {
        return friendService.block(profile.getProfileId(), profileId);
    }

    @DeleteMapping("/blocks/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER','ADMIN') and @accessGuard.isNotCurrentProfile(#profileId)")
    public void unblock(
        @AuthenticationPrincipal AuthenticatedProfile profile,
        @PathVariable UUID profileId
    ) {
        friendService.unblock(profile.getProfileId(), profileId);
    }
}
