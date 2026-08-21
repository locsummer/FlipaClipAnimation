package com.flipaclip.animation.data.model

import java.io.Serializable
import java.util.UUID

enum class JointType {
    HEAD,
    NECK,
    CHEST,
    HIP,
    LEFT_SHOULDER,
    LEFT_ELBOW,
    LEFT_HAND,
    RIGHT_SHOULDER,
    RIGHT_ELBOW,
    RIGHT_HAND,
    LEFT_HIP_JOINT,
    LEFT_KNEE,
    LEFT_FOOT,
    RIGHT_HIP_JOINT,
    RIGHT_KNEE,
    RIGHT_FOOT,
    CUSTOM
}

data class JointNode(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var x: Float,
    var y: Float,
    var parentId: String? = null,
    var type: JointType = JointType.CUSTOM,
    var radius: Float = 14f,
    var color: Int = 0xFF212121.toInt(),
    var strokeThickness: Float = 12f
) : Serializable {
    fun deepCopy(): JointNode {
        return JointNode(
            id = id,
            name = name,
            x = x,
            y = y,
            parentId = parentId,
            type = type,
            radius = radius,
            color = color,
            strokeThickness = strokeThickness
        )
    }
}

data class BoneConnection(
    val id: String = UUID.randomUUID().toString(),
    val startJointId: String,
    val endJointId: String,
    var thickness: Float = 12f,
    var color: Int = 0xFF212121.toInt()
) : Serializable {
    fun deepCopy(): BoneConnection {
        return BoneConnection(
            id = id,
            startJointId = startJointId,
            endJointId = endJointId,
            thickness = thickness,
            color = color
        )
    }
}

data class SkeletonPuppet(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Stickman",
    var isSelected: Boolean = true,
    var isLocked: Boolean = false,
    var isVisible: Boolean = true,
    var rootX: Float = 540f,
    var rootY: Float = 960f,
    var headRadius: Float = 48f,
    var color: Int = 0xFF212121.toInt(),
    var strokeWidth: Float = 12f,
    val joints: MutableList<JointNode> = mutableListOf(),
    val bones: MutableList<BoneConnection> = mutableListOf()
) : Serializable {

    fun deepCopy(): SkeletonPuppet {
        return SkeletonPuppet(
            id = id,
            name = name,
            isSelected = isSelected,
            isLocked = isLocked,
            isVisible = isVisible,
            rootX = rootX,
            rootY = rootY,
            headRadius = headRadius,
            color = color,
            strokeWidth = strokeWidth,
            joints = joints.map { it.deepCopy() }.toMutableList(),
            bones = bones.map { it.deepCopy() }.toMutableList()
        )
    }

    companion object {
        fun createDefaultStickman(centerX: Float = 540f, centerY: Float = 960f, color: Int = 0xFF212121.toInt()): SkeletonPuppet {
            val puppet = SkeletonPuppet(
                rootX = centerX,
                rootY = centerY,
                color = color,
                strokeWidth = 12f
            )

            val head = JointNode(name = "Head", x = centerX, y = centerY - 280f, type = JointType.HEAD, radius = 20f, color = color)
            val neck = JointNode(name = "Neck", x = centerX, y = centerY - 210f, parentId = head.id, type = JointType.NECK, radius = 12f, color = color)
            val chest = JointNode(name = "Chest", x = centerX, y = centerY - 140f, parentId = neck.id, type = JointType.CHEST, radius = 12f, color = color)
            val hip = JointNode(name = "Hip", x = centerX, y = centerY, parentId = chest.id, type = JointType.HIP, radius = 16f, color = color)

            // Arms
            val lShoulder = JointNode(name = "L_Shoulder", x = centerX - 60f, y = centerY - 140f, parentId = chest.id, type = JointType.LEFT_SHOULDER, radius = 12f, color = color)
            val lElbow = JointNode(name = "L_Elbow", x = centerX - 120f, y = centerY - 80f, parentId = lShoulder.id, type = JointType.LEFT_ELBOW, radius = 12f, color = color)
            val lHand = JointNode(name = "L_Hand", x = centerX - 180f, y = centerY - 20f, parentId = lElbow.id, type = JointType.LEFT_HAND, radius = 14f, color = color)

            val rShoulder = JointNode(name = "R_Shoulder", x = centerX + 60f, y = centerY - 140f, parentId = chest.id, type = JointType.RIGHT_SHOULDER, radius = 12f, color = color)
            val rElbow = JointNode(name = "R_Elbow", x = centerX + 120f, y = centerY - 80f, parentId = rShoulder.id, type = JointType.RIGHT_ELBOW, radius = 12f, color = color)
            val rHand = JointNode(name = "R_Hand", x = centerX + 180f, y = centerY - 20f, parentId = rElbow.id, type = JointType.RIGHT_HAND, radius = 14f, color = color)

            // Legs
            val lHip = JointNode(name = "L_Hip", x = centerX - 40f, y = centerY + 30f, parentId = hip.id, type = JointType.LEFT_HIP_JOINT, radius = 12f, color = color)
            val lKnee = JointNode(name = "L_Knee", x = centerX - 80f, y = centerY + 160f, parentId = lHip.id, type = JointType.LEFT_KNEE, radius = 12f, color = color)
            val lFoot = JointNode(name = "L_Foot", x = centerX - 110f, y = centerY + 300f, parentId = lKnee.id, type = JointType.LEFT_FOOT, radius = 14f, color = color)

            val rHip = JointNode(name = "R_Hip", x = centerX + 40f, y = centerY + 30f, parentId = hip.id, type = JointType.RIGHT_HIP_JOINT, radius = 12f, color = color)
            val rKnee = JointNode(name = "R_Knee", x = centerX + 80f, y = centerY + 160f, parentId = rHip.id, type = JointType.RIGHT_KNEE, radius = 12f, color = color)
            val rFoot = JointNode(name = "R_Foot", x = centerX + 110f, y = centerY + 300f, parentId = rKnee.id, type = JointType.RIGHT_FOOT, radius = 14f, color = color)

            puppet.joints.addAll(
                listOf(
                    head, neck, chest, hip,
                    lShoulder, lElbow, lHand,
                    rShoulder, rElbow, rHand,
                    lHip, lKnee, lFoot,
                    rHip, rKnee, rFoot
                )
            )

            puppet.bones.addAll(
                listOf(
                    // Spine
                    BoneConnection(startJointId = neck.id, endJointId = chest.id, color = color, thickness = 14f),
                    BoneConnection(startJointId = chest.id, endJointId = hip.id, color = color, thickness = 14f),

                    // Left Arm
                    BoneConnection(startJointId = chest.id, endJointId = lShoulder.id, color = color, thickness = 12f),
                    BoneConnection(startJointId = lShoulder.id, endJointId = lElbow.id, color = color, thickness = 12f),
                    BoneConnection(startJointId = lElbow.id, endJointId = lHand.id, color = color, thickness = 12f),

                    // Right Arm
                    BoneConnection(startJointId = chest.id, endJointId = rShoulder.id, color = color, thickness = 12f),
                    BoneConnection(startJointId = rShoulder.id, endJointId = rElbow.id, color = color, thickness = 12f),
                    BoneConnection(startJointId = rElbow.id, endJointId = rHand.id, color = color, thickness = 12f),

                    // Left Leg
                    BoneConnection(startJointId = hip.id, endJointId = lHip.id, color = color, thickness = 14f),
                    BoneConnection(startJointId = lHip.id, endJointId = lKnee.id, color = color, thickness = 14f),
                    BoneConnection(startJointId = lKnee.id, endJointId = lFoot.id, color = color, thickness = 14f),

                    // Right Leg
                    BoneConnection(startJointId = hip.id, endJointId = rHip.id, color = color, thickness = 14f),
                    BoneConnection(startJointId = rHip.id, endJointId = rKnee.id, color = color, thickness = 14f),
                    BoneConnection(startJointId = rKnee.id, endJointId = rFoot.id, color = color, thickness = 14f)
                )
            )

            return puppet
        }
    }
}
