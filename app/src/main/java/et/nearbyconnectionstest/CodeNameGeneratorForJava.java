package et.nearbyconnectionstest;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by William on 2018/2/12.
 */

public class CodeNameGeneratorForJava {

    private static String[] COLORS = new String[]{"Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet", "Purple", "Lavender", "Fuchsia", "Plum", "Orchid", "Magenta"};

    private static String[] TREATS = new String[]{"Alpha", "Beta", "Cupcake", "Donut", "Eclair", "Froyo", "Gingerbread", "Honeycomb", "Ice Cream Sandwich", "Jellybean", "Kit Kat", "Lollipop", "Marshmallow", "Nougat"};

    private static Random generator = new Random();

    /** Generate a random Android agent codename  */
    public static String generate() {
        String color = COLORS[generator.nextInt(COLORS.length)];
        String treat = TREATS[generator.nextInt(TREATS.length)];
        return color + " " + treat;
    }

}
