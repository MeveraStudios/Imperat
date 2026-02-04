package studio.mevera.imperat.selector.field.operators;

import java.util.Set;

interface OperatorFields {

    OperatorField<SortOption> SORT = new SortOperatorField("sort");

    OperatorField<Integer> LIMIT = new LimitOperatorField("limit");

    Set<OperatorField<?>> ALL_OPERATORS = Set.of(
            SORT,
            LIMIT
    );
}
