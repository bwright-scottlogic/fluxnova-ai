package com.fluxnova.scottlogic.mcp.tools;

import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.identity.Group;
import org.finos.fluxnova.bpm.engine.identity.User;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IdentityTools {

    private final IdentityService identityService;

    public IdentityTools(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Tool(description = "List users, optionally filtered by group ID or a first/last name search string.")
    public List<Map<String, String>> listUsers(
            @ToolParam(description = "Optional group ID to list members of", required = false) String groupId,
            @ToolParam(description = "Optional full-text search on first/last name", required = false) String nameLike) {

        var query = identityService.createUserQuery();
        if (groupId != null && !groupId.isBlank()) {
            query.memberOfGroup(groupId);
        }
        if (nameLike != null && !nameLike.isBlank()) {
            query.userLastNameLike("%" + nameLike + "%");
        }

        return query.list().stream()
                .map(u -> Map.of(
                        "id", u.getId() != null ? u.getId() : "",
                        "firstName", u.getFirstName() != null ? u.getFirstName() : "",
                        "lastName", u.getLastName() != null ? u.getLastName() : "",
                        "email", u.getEmail() != null ? u.getEmail() : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "Create a new user with the given ID, name, email and password.")
    public void createUser(
            @ToolParam(description = "Unique user ID (login name)") String userId,
            @ToolParam(description = "First name of the user") String firstName,
            @ToolParam(description = "Last name of the user") String lastName,
            @ToolParam(description = "Email address of the user") String email,
            @ToolParam(description = "Initial password for the user") String password) {

        User user = identityService.newUser(userId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(password);
        identityService.saveUser(user);
    }

    @Tool(description = "Delete a user by their user ID.")
    public void deleteUser(
            @ToolParam(description = "The user ID to delete") String userId) {

        identityService.deleteUser(userId);
    }

    @Tool(description = "Unlock a user account that has been locked due to failed login attempts.")
    public void unlockUser(
            @ToolParam(description = "The user ID to unlock") String userId) {

        identityService.unlockUser(userId);
    }

    @Tool(description = "List all groups, optionally filtered by a member user ID.")
    public List<Map<String, String>> listGroups(
            @ToolParam(description = "Optional user ID to list groups the user is a member of", required = false) String userId) {

        var query = identityService.createGroupQuery();
        if (userId != null && !userId.isBlank()) {
            query.groupMember(userId);
        }

        return query.list().stream()
                .map(g -> Map.of(
                        "id", g.getId() != null ? g.getId() : "",
                        "name", g.getName() != null ? g.getName() : "",
                        "type", g.getType() != null ? g.getType() : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "Create a new group with the given ID and name.")
    public void createGroup(
            @ToolParam(description = "Unique group ID") String groupId,
            @ToolParam(description = "Human-readable group name") String groupName) {

        Group group = identityService.newGroup(groupId);
        group.setName(groupName);
        identityService.saveGroup(group);
    }

    @Tool(description = "Delete a group by its group ID.")
    public void deleteGroup(
            @ToolParam(description = "The group ID to delete") String groupId) {

        identityService.deleteGroup(groupId);
    }

    @Tool(description = "Add a user to a group.")
    public void addUserToGroup(
            @ToolParam(description = "The user ID to add") String userId,
            @ToolParam(description = "The group ID to add the user to") String groupId) {

        identityService.createMembership(userId, groupId);
    }

    @Tool(description = "Remove a user from a group.")
    public void removeUserFromGroup(
            @ToolParam(description = "The user ID to remove") String userId,
            @ToolParam(description = "The group ID to remove the user from") String groupId) {

        identityService.deleteMembership(userId, groupId);
    }

    @Tool(description = "Check whether the given credentials are valid for a user. Returns true if authentication succeeds.")
    public boolean checkUserPassword(
            @ToolParam(description = "The user ID to authenticate") String userId,
            @ToolParam(description = "The password to check") String password) {

        return identityService.checkPassword(userId, password);
    }
}
