package com.ssafy.modaba.data.api.mock

import com.ssafy.modaba.data.model.FriendshipStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

internal object MockDataStore {
    data class MockUser(
        val id: Long,
        val email: String,
        var password: String,
        var nickname: String,
        var imageLink: String?
    )

    data class MockFriendship(
        val fromUserId: Long,
        val toUserId: Long,
        var status: FriendshipStatus
    )

    private val idCounter = AtomicLong(1)
    private val usersByEmail = ConcurrentHashMap<String, MockUser>()
    private val usersById = ConcurrentHashMap<Long, MockUser>()
    private val friendships = CopyOnWriteArrayList<MockFriendship>()
    private val resetTokens = ConcurrentHashMap<String, String>()

    init {
        val user = createUserInternal(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, DEFAULT_IMAGE_LINK)
        val friend = createUserInternal("friend@test.com", "test1234", "MockFriend", null)
        val pending = createUserInternal("pending@test.com", "test1234", "PendingFriend", null)
        friendships.add(MockFriendship(friend.id, user.id, FriendshipStatus.ACCEPTED))
        friendships.add(MockFriendship(pending.id, user.id, FriendshipStatus.PENDING))
    }

    fun createUser(email: String, password: String, nickname: String, imageLink: String? = null): MockUser? {
        if (usersByEmail.containsKey(email)) return null
        return createUserInternal(email, password, nickname, imageLink)
    }

    fun getUserByEmail(email: String): MockUser? = usersByEmail[email]

    fun getUserById(id: Long): MockUser? = usersById[id]

    fun isNicknameTaken(nickname: String, excludeId: Long? = null): Boolean {
        return usersById.values.any { it.nickname == nickname && it.id != excludeId }
    }

    fun removeUser(user: MockUser) {
        usersByEmail.remove(user.email)
        usersById.remove(user.id)
        friendships.removeAll { it.fromUserId == user.id || it.toUserId == user.id }
    }

    fun listFriends(userId: Long): List<MockUser> {
        val friendIds = friendships
            .filter { it.status == FriendshipStatus.ACCEPTED && (it.fromUserId == userId || it.toUserId == userId) }
            .map { if (it.fromUserId == userId) it.toUserId else it.fromUserId }
        return friendIds.mapNotNull { usersById[it] }
    }

    fun listPending(userId: Long): List<MockUser> {
        val pendingIds = friendships
            .filter { it.status == FriendshipStatus.PENDING && it.toUserId == userId }
            .map { it.fromUserId }
        return pendingIds.mapNotNull { usersById[it] }
    }

    fun addFriendRequest(fromId: Long, toId: Long): Boolean {
        if (fromId == toId) return false
        if (getUserById(fromId) == null || getUserById(toId) == null) return false
        if (findFriendshipBetween(fromId, toId) != null) return false
        friendships.add(MockFriendship(fromId, toId, FriendshipStatus.PENDING))
        return true
    }

    fun respondToRequest(fromId: Long, toId: Long, status: FriendshipStatus): Boolean {
        val friendship = findFriendship(fromId, toId) ?: return false
        if (friendship.status != FriendshipStatus.PENDING) return false
        friendship.status = status
        return true
    }

    fun removeFriendshipBetween(userId: Long, otherId: Long): Boolean {
        val friendship = findFriendshipBetween(userId, otherId) ?: return false
        if (friendship.status != FriendshipStatus.ACCEPTED) return false
        friendships.remove(friendship)
        return true
    }

    fun buildToken(type: String, email: String): String {
        val timestamp = System.currentTimeMillis()
        return "mock:$type:$email:$timestamp"
    }

    fun parseToken(token: String?, expectedType: String): String? {
        if (token.isNullOrBlank()) return null
        val parts = token.split(':')
        if (parts.size < 4) return null
        if (parts[0] != "mock" || parts[1] != expectedType) return null
        return parts[2]
    }

    fun getUserFromToken(token: String?, expectedType: String): MockUser? {
        val email = parseToken(token, expectedType) ?: return null
        return getUserByEmail(email)
    }

    fun createResetToken(email: String): String? {
        val user = getUserByEmail(email) ?: return null
        val token = "mock-reset:${user.email}:${System.currentTimeMillis()}"
        resetTokens[token] = user.email
        return token
    }

    fun consumeResetToken(token: String): String? = resetTokens.remove(token)

    private fun createUserInternal(
        email: String,
        password: String,
        nickname: String,
        imageLink: String?
    ): MockUser {
        val user = MockUser(
            idCounter.getAndIncrement(),
            email,
            password,
            nickname,
            imageLink
        )
        usersByEmail[email] = user
        usersById[user.id] = user
        return user
    }

    private fun findFriendship(fromId: Long, toId: Long): MockFriendship? {
        return friendships.firstOrNull { it.fromUserId == fromId && it.toUserId == toId }
    }

    private fun findFriendshipBetween(userId: Long, otherId: Long): MockFriendship? {
        return friendships.firstOrNull {
            (it.fromUserId == userId && it.toUserId == otherId) ||
                (it.fromUserId == otherId && it.toUserId == userId)
        }
    }

    private const val TEST_EMAIL = "test@test.com"
    private const val TEST_PASSWORD = "test1234"
    private const val TEST_NICKNAME = "MockUser"
    private const val DEFAULT_IMAGE_LINK = "https://example.com/mock-user.png"
}
