/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.pathing.path;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.pathing.movement.IMovement;
import baritone.utils.Utils;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.List;

/**
 * @author leijurv
 */
public interface IPath {

    /**
     * Ordered list of movements to carry out.
     * movements.get(i).getSrc() should equal positions.get(i)
     * movements.get(i).getDest() should equal positions.get(i+1)
     * movements.size() should equal positions.size()-1
     */
    List<IMovement> movements();

    /**
     * All positions along the way.
     * Should begin with the same as getSrc and end with the same as getDest
     */
    List<BetterBlockPos> positions();

    /**
     * This path is actually going to be executed in the world. Do whatever additional processing is required.
     * (as opposed to Path objects that are just constructed every frame for rendering)
     */
    default void postprocess() {}

    /**
     * Number of positions in this path
     *
     * @return Number of positions in this path
     */
    default int length() {
        return positions().size();
    }

    /**
     * What goal was this path calculated towards?
     *
     * @return
     */
    Goal getGoal();

    default Tuple<Double, BlockPos> closestPathPos() {
        double best = -1;
        BlockPos bestPos = null;
        for (BlockPos pos : positions()) {
            double dist = Utils.playerDistanceToCenter(pos);
            if (dist < best || best == -1) {
                best = dist;
                bestPos = pos;
            }
        }
        return new Tuple<>(best, bestPos);
    }

    /**
     * Where does this path start
     */
    default BetterBlockPos getSrc() {
        return positions().get(0);
    }

    /**
     * Where does this path end
     */
    default BetterBlockPos getDest() {
        List<BetterBlockPos> pos = positions();
        return pos.get(pos.size() - 1);
    }

    default double ticksRemainingFrom(int pathPosition) {
        double sum = 0;
        //this is fast because we aren't requesting recalculation, it's just cached
        for (int i = pathPosition; i < movements().size(); i++) {
            sum += movements().get(i).getCost();
        }
        return sum;
    }

    int getNumNodesConsidered();

    default CutoffResult cutoffAtLoadedChunks() {
        for (int i = 0; i < positions().size(); i++) {
            BlockPos pos = positions().get(i);
            if (Minecraft.getMinecraft().world.getChunk(pos) instanceof EmptyChunk) {
                return CutoffResult.cutoffPath(this, i);
            }
        }
        return CutoffResult.preservePath(this);
    }

    default CutoffResult staticCutoff(Goal destination) {
        if (length() < BaritoneAPI.getSettings().pathCutoffMinimumLength.get()) {
            return CutoffResult.preservePath(this);
        }
        if (destination == null || destination.isInGoal(getDest())) {
            return CutoffResult.preservePath(this);
        }
        double factor = BaritoneAPI.getSettings().pathCutoffFactor.get();
        int newLength = (int) (length() * factor);
        return CutoffResult.cutoffPath(this, newLength);
    }
}
