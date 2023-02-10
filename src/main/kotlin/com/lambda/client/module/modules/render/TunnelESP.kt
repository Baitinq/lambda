package com.lambda.client.module.modules.render

import com.lambda.client.event.SafeClientEvent
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.module.Category
import com.lambda.client.module.Module
import com.lambda.client.util.TickTimer
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.util.graphics.ESPRenderer
import com.lambda.client.util.graphics.GeometryMasks
import com.lambda.client.util.math.VectorUtils.distanceTo
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import kotlinx.coroutines.launch
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

object TunnelESP : Module(
    name = "TunnelESP",
    description = "Highlights tunnels",
    category = Category.RENDER
) {
    private val filled by setting("Filled", true)
    private val outline by setting("Outline", true)
    private val color by setting("Color", ColorHolder(148, 161, 255), false)
    private val aFilled by setting("Filled Alpha", 127, 0..255, 1)
    private val aOutline by setting("Outline Alpha", 255, 0..255, 1)
    private val range by setting("Range", 128, 0..256, 8, unit = " blocks")
    private val minimumTunnelLength = 0

    private val renderer = ESPRenderer()
    private val timer = TickTimer()

    init {
        safeListener<RenderWorldEvent> {
            if (timer.tick(133L)) { // Avoid running this on a tick
                updateRenderer()
            }
            renderer.render(false)
        }
    }

    private fun SafeClientEvent.updateRenderer() {
        renderer.aFilled = if (filled) aFilled else 0
        renderer.aOutline = if (outline) aOutline else 0

        val color = color.clone()

        defaultScope.launch {
            val cached = ArrayList<Triple<AxisAlignedBB, ColorHolder, Int>>()

            for (x in -range..range) for (y in -range .. range) for (z in -range..range) {
                val pos = BlockPos(player.posX + x, player.posY + y, player.posZ + z)
                if (player.distanceTo(pos) > range) continue
                if (!isTunnel(pos)) continue

                cached.add(Triple(AxisAlignedBB(pos), color, GeometryMasks.Quad.ALL))
            }

            renderer.replaceAll(cached)
        }
    }

    private fun SafeClientEvent.isTunnel(pos: BlockPos) =
        //works, but need to make it configurable and highligh block on top too
        //what about vertical tunnels?
            world.isAirBlock(pos)
            && world.isAirBlock(pos.up())
            && ((!world.isAirBlock(pos.east())
                && !world.isAirBlock(pos.west())) || (!world.isAirBlock(pos.south())
                && !world.isAirBlock(pos.north())))
}
