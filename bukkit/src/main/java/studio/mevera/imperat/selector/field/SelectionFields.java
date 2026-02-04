package studio.mevera.imperat.selector.field;

import studio.mevera.imperat.BukkitUtil;
import studio.mevera.imperat.selector.field.filters.PredicateField;
import studio.mevera.imperat.selector.field.operators.OperatorField;

import java.util.HashSet;
import java.util.Set;

//minestom style ;D
interface SelectionFields {

    //TODO add constants for each type of field

    Set<SelectionField<?>> ALL = BukkitUtil.mergedSet(
            new HashSet<>(PredicateField.ALL_PREDICATES),
            new HashSet<>(OperatorField.ALL_OPERATORS),
            HashSet::new
    );

}
