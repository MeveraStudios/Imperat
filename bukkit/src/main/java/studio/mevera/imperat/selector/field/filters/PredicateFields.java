package studio.mevera.imperat.selector.field.filters;

import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import studio.mevera.imperat.selector.field.Range;

import java.util.Set;

/**
 * The PredicateFields interface defines a set of named PredicateField objects
 * used for filtering entities based on various criteria.
 * These predicates can be used to generate filtering conditions
 * which can test entities against specified values.
 */
interface PredicateFields {


    //TODO predicates here
    PredicateField<String> NAME = new NameField("name");

    PredicateField<EntityType> TYPE = new TypeField("type");

    PredicateField<Range<Integer>> LEVEL = new LevelField("level");

    PredicateField<Range<Double>> DISTANCE = new DistanceField("distance");

    PredicateField<GameMode> GAMEMODE = new GamemodeField("gamemode");

    PredicateField<String> TAG = new TagField("tag");


    Set<PredicateField<?>> ALL_PREDICATES = Set.of(
        NAME,
        TYPE,
        LEVEL,
        DISTANCE,
        GAMEMODE,
        TAG
    );
    //TODO add more

}
