package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.Expression;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** The textures of one mob: one per colour variant and facial expression. */
public final class ExpressionTextures {
    private final Identifier[][] table;

    /**
     * @param mob       texture folder under textures/entity
     * @param variants  texture base names of the colour variants (e.g. "bulb_sky")
     * @param painted   the expressions tools/mobs.py paints for this mob
     */
    public ExpressionTextures(String mob, String[] variants, Expression... painted) {
        Set<Expression> have = EnumSet.of(Expression.NEUTRAL);
        have.addAll(java.util.List.of(painted));
        Expression[] all = Expression.values();
        this.table = new Identifier[variants.length][all.length];
        for (int v = 0; v < variants.length; v++) {
            for (Expression e : all) {
                Expression use = e;
                while (!have.contains(use)) {
                    use = use.fallback();
                }
                this.table[v][e.ordinal()] = TheSift.id("textures/entity/" + mob + "/" + variants[v] + use.suffix() + ".png");
            }
        }
    }

    public static ExpressionTextures single(String mob, Expression... painted) {
        return new ExpressionTextures(mob, new String[]{mob}, painted);
    }

    public Identifier get(int variant, Expression e) {
        Identifier[] row = this.table[Math.max(0, Math.min(this.table.length - 1, variant))];
        return row[e.ordinal()];
    }

    public Identifier get(Expression e) {
        return this.get(0, e);
    }
}
