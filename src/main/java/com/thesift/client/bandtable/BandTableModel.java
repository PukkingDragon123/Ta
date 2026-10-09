package com.thesift.client.bandtable;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * F2 Band Table: the table's moving parts (tools/band_table.py; its still body is the block model, so the 'base*'
 * parts are never drawn here). The songbook opens and turns its pages, the metronome ticks, the chimes and the
 * horn sway to the music, the sticks tap the drum - and the glowing notes of the song being played hang over the
 * table, each lighting up as it is played.
 *
 * <p>Drawn twice: once with the painted texture, once with the glow texture ({@link Pass#glow}), where only the lit
 * notes show.
 */
public class BandTableModel extends Model<BandTableModel.Pass> {
    public static final int NOTES = 8;

    /** One drawing of the model: the table's state, and whether this is the glow pass. */
    public record Pass(BandTableRenderer.State s, boolean glow) {
    }

    private final ModelPart[] base;
    private final ModelPart book;
    private final ModelPart bookLeft;
    private final ModelPart bookRight;
    private final ModelPart page;
    private final ModelPart pendulum;
    private final ModelPart[] sticks = new ModelPart[2];
    private final ModelPart[] chimes = new ModelPart[3];
    private final ModelPart horn;
    private final ModelPart flare;
    private final ModelPart[] notes = new ModelPart[NOTES];

    public BandTableModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        String[] still = {"base_plinth", "base_body", "base_post_0", "base_post_1", "base_post_2", "base_post_3", "base_top", "base_drum",
                "base_metronome", "base_chime_tree", "base_horn_box", "base_stand"};
        this.base = new ModelPart[still.length];
        for (int i = 0; i < still.length; i++) {
            this.base[i] = root.getChild(still[i]);
        }
        this.book = root.getChild("book");
        this.bookLeft = this.book.getChild("book_left");
        this.bookRight = this.book.getChild("book_right");
        this.page = this.book.getChild("book_page");
        this.pendulum = root.getChild("pendulum");
        for (int i = 0; i < this.sticks.length; i++) {
            this.sticks[i] = root.getChild("stick_" + i);
        }
        for (int i = 0; i < this.chimes.length; i++) {
            this.chimes[i] = root.getChild("chime_" + i);
        }
        this.horn = root.getChild("horn");
        this.flare = this.horn.getChild("horn_flare");
        for (int i = 0; i < NOTES; i++) {
            this.notes[i] = root.getChild("note_" + i);
        }
    }

    @Override
    public void setupAnim(Pass pass) {
        super.setupAnim(pass);
        BandTableRenderer.State s = pass.s();
        boolean glow = pass.glow();
        float t = s.time;
        for (ModelPart p : this.base) {
            p.visible = false;
        }
        // in the glow pass only the notes show
        this.book.visible = !glow;
        this.pendulum.visible = !glow;
        this.horn.visible = !glow;
        for (ModelPart p : this.sticks) {
            p.visible = !glow;
        }
        for (ModelPart p : this.chimes) {
            p.visible = !glow;
        }

        // the songbook: wide open while a song is played, its page turning over with every note
        float shut = 1.0F - s.open;
        this.bookLeft.zRot = 0.12F + shut * 1.25F;
        this.bookRight.zRot = -0.12F - shut * 1.25F;
        float f = Mth.frac(s.flip);
        this.page.zRot = -(0.14F + f * (Mth.PI - 0.28F));
        this.page.visible = !glow && s.open > 0.5F;
        this.book.xRot += Mth.sin(t * 0.05F) * 0.03F;

        // the metronome ticks back and forth, the chimes sway (harder while music plays)
        this.pendulum.zRot = Mth.sin(s.swing) * 0.55F;
        float sway = s.performing ? 0.22F : 0.07F;
        for (int i = 0; i < this.chimes.length; i++) {
            this.chimes[i].zRot = Mth.sin(s.swing * 1.3F + i * 1.7F) * sway;
            this.chimes[i].xRot = Mth.cos(s.swing * 1.1F + i * 2.3F) * sway * 0.6F;
        }
        // the horn turns slowly to the room, and swells as each note lands
        this.horn.yRot = Mth.sin(t * 0.025F) * 0.45F;
        float swell = 1.0F + 0.22F * s.hit;
        this.flare.xScale = swell;
        this.flare.yScale = swell;
        this.flare.zScale = swell;
        // the sticks tap the drum while a song is played (a slam on every note that lands)
        for (int i = 0; i < this.sticks.length; i++) {
            float lift = 0.0F;
            if (s.performing) {
                lift = Math.max(0.0F, Mth.sin(t * 0.7F + i * Mth.PI)) * 0.45F;
                if (s.hit > 0.0F && i == (s.progress & 1)) {
                    lift = (1.0F - s.hit) * 0.6F;
                }
            }
            this.sticks[i].xRot -= lift;
        }

        // the notes
        for (int i = 0; i < NOTES; i++) {
            this.note(i, s, glow);
        }
    }

    private void note(int i, BandTableRenderer.State s, boolean glow) {
        ModelPart n = this.notes[i];
        float t = s.time;
        int count = s.notes.length;
        if (count > 0 && i < count) {
            // the song, written left to right over the table, each note as high as it sounds
            boolean played = i < s.progress;
            boolean next = i == s.progress && s.performing;
            float spacing = Math.min(1.6F, 11.2F / Math.max(1, count - 1));
            n.x = -spacing * (count - 1) / 2.0F + i * spacing;
            n.y = 1.0F - s.notes[i] * 0.2F + Mth.sin(t * 0.09F + i * 0.8F) * 0.25F;
            n.z = 1.0F;
            n.yRot = Mth.sin(t * 0.05F + i) * 0.35F;
            float scale = 0.42F;
            if (played) {
                scale = 0.52F;
                if (i == s.progress - 1) {
                    scale += s.hit * 0.3F;
                }
            } else if (next) {
                scale = 0.46F + Mth.sin(t * 0.4F) * 0.05F;
                n.x += s.miss * Mth.sin(t * 2.7F) * 0.5F;
            }
            if (!s.performing && s.done > 0.0F) {
                // the song is over: the notes rise and fade away (bright if it was won)
                n.y -= (1.0F - s.done) * 6.0F;
                scale *= s.done;
            }
            n.xScale = scale;
            n.yScale = scale;
            n.zScale = scale;
            // a failed song's notes fade out dark
            boolean lit = next || played && (s.performing || s.doneLevel > 0);
            n.visible = scale > 0.02F && (!glow || lit);
        } else if (count == 0 && i % 3 == 0) {
            // idle: three notes drift round the table
            float a = t * 0.03F + i * 2.1F;
            n.x = Mth.cos(a) * 5.0F;
            n.z = Mth.sin(a) * 4.0F;
            n.y = -0.5F + Mth.sin(t * 0.07F + i) * 0.8F;
            n.yRot = -a + Mth.HALF_PI;
            n.xScale = 0.32F;
            n.yScale = 0.32F;
            n.zScale = 0.32F;
            n.visible = true;
        } else {
            n.visible = false;
        }
    }
}
