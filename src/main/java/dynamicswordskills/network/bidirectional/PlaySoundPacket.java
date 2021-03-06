/**
    Copyright (C) <2017> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package dynamicswordskills.network.bidirectional;

import java.io.IOException;

import org.apache.commons.lang3.Validate;

import dynamicswordskills.network.AbstractMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 
 * Plays a sound on the client or server side
 *
 */
public class PlaySoundPacket extends AbstractMessage<PlaySoundPacket>
{
	private SoundEvent sound;
	private SoundCategory category;
	private float volume;
	private float pitch;
	/** Coordinates at which to play the sound; used on the server side */
	private double x, y, z;

	public PlaySoundPacket() {}

	public PlaySoundPacket(SoundEvent sound, SoundCategory category, float volume, float pitch, double x, double y, double z) {
		Validate.notNull(sound, "sound", new Object[0]);
		this.sound = sound;
		this.category = category;
		this.volume = volume;
		this.pitch = pitch;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Use only when sending to the SERVER to use the entity's coordinates as the center;
	 * if sent to the client, the position coordinates will be ignored.
	 */
	public PlaySoundPacket(SoundEvent sound, SoundCategory category, float volume, float pitch, Entity entity) {
		this(sound, category, volume, pitch, entity.posX, entity.posY, entity.posZ);
	}

	/**
	 * Use only when sending to the CLIENT - the sound will play at the player's position
	 */
	public PlaySoundPacket(SoundEvent sound, SoundCategory category, float volume, float pitch) {
		this(sound, category, volume, pitch, 0, 0, 0);
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		this.sound = (SoundEvent) SoundEvent.REGISTRY.getObjectById(buffer.readVarInt());
		this.category = (SoundCategory) buffer.readEnumValue(SoundCategory.class);
		volume = buffer.readFloat();
		pitch = buffer.readFloat();
		x = buffer.readDouble();
		y = buffer.readDouble();
		z = buffer.readDouble();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeVarInt(SoundEvent.REGISTRY.getIDForObject(this.sound));
		buffer.writeEnumValue(this.category);
		buffer.writeFloat(volume);
		buffer.writeFloat(pitch);
		buffer.writeDouble(x);
		buffer.writeDouble(y);
		buffer.writeDouble(z);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		if (side.isClient()) {
			player.playSound(sound, volume, pitch);
		} else {
			// pass 'null' player so they will hear the sound, too
			player.getEntityWorld().playSound(null, x, y, z, sound, category, volume, pitch);
		}
	}
}
