package com.metrovice.rp.character;

public record CharacterProfile(int id, String ownerUuid, String name, int age, String createdAt) {}