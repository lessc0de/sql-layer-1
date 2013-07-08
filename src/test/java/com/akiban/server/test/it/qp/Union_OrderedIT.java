/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.akiban.server.test.it.qp;

import com.akiban.qp.expression.IndexBound;
import com.akiban.qp.expression.IndexKeyRange;
import com.akiban.qp.operator.API;
import com.akiban.qp.operator.ExpressionGenerator;
import com.akiban.qp.operator.Operator;
import com.akiban.qp.row.RowBase;
import com.akiban.qp.rowtype.IndexRowType;
import com.akiban.qp.rowtype.RowType;
import com.akiban.qp.rowtype.Schema;
import com.akiban.server.api.dml.SetColumnSelector;
import com.akiban.server.api.dml.scan.NewRow;
import org.junit.Test;

import static com.akiban.qp.operator.API.*;
import static com.akiban.server.test.ExpressionGenerators.field;
import static junit.framework.Assert.fail;

public class Union_OrderedIT extends OperatorITBase
{
    @Override
    protected void setupCreateSchema()
    {
        t = createTable(
            "schema", "t",
            "pid int not null primary key",
            "x int");
        createIndex("schema", "t", "idx_x", "x");
    }

    @Override
    protected void setupPostCreateSchema()
    {
        schema = new Schema(ais());
        tRowType = schema.userTableRowType(userTable(t));
        tPidIndexRowType = indexType(t, "pid");
        tXIndexRowType = indexType(t, "x");
        coi = group(t);
        adapter = newStoreAdapter(schema);
        queryContext = queryContext(adapter);
        db = new NewRow[] {
            createNewRow(t, 1000L, 1L),
            createNewRow(t, 1001L, 1L),
            createNewRow(t, 1002L, 1L),
            createNewRow(t, 2000L, 2L),
            createNewRow(t, 2001L, 2L),
            createNewRow(t, 2002L, 2L),
        };
        use(db);
    }

    private int t;
    private RowType tRowType;
    private IndexRowType tPidIndexRowType;
    private IndexRowType tXIndexRowType;

    // IllegalArumentException tests

    @Test
    public void testInputNull()
    {
        // First input null
        try {
            union_Ordered(null,
                          groupScan_Default(coi),
                          tXIndexRowType,
                          tXIndexRowType,
                          1,
                          1,
                          ascending(true),
                          false);
        } catch (IllegalArgumentException e) {
        }
        // Second input null
        try {
            union_Ordered(groupScan_Default(coi),
                          null,
                          tXIndexRowType,
                          tXIndexRowType,
                          1,
                          1,
                          ascending(true),
                          false);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testInputType()
    {
        // First input type null
        try {
            union_Ordered(groupScan_Default(coi),
                          groupScan_Default(coi),
                          null,
                          tXIndexRowType,
                          1,
                          1,
                          ascending(true),
                          false);
            fail();
        } catch (IllegalArgumentException e) {
        }
        // Second input type null
        try {
            union_Ordered(groupScan_Default(coi),
                          groupScan_Default(coi),
                          tXIndexRowType,
                          null,
                          1,
                          1,
                          ascending(true),
                          false);
            fail();
        } catch (IllegalArgumentException e) {
        }
        // Test different input types
        try {
            union_Ordered(groupScan_Default(coi),
                          groupScan_Default(coi),
                          tXIndexRowType,
                          tPidIndexRowType,
                          1,
                          1,
                          ascending(true),
                          false);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testOrderingColumns()
    {
        // First ordering fields negative
        try {
            union_Ordered(groupScan_Default(coi),
                          groupScan_Default(coi),
                          tXIndexRowType,
                          tXIndexRowType,
                          -1,
                          1,
                          ascending(true),
                          false);
            fail();
        } catch (IllegalArgumentException e) {
        }
        // Second ordering fields negative
        try {
            union_Ordered(groupScan_Default(coi),
                          groupScan_Default(coi),
                          tXIndexRowType,
                          tXIndexRowType,
                          1,
                          -1,
                          ascending(true),
                          false);
            fail();
        } catch (IllegalArgumentException e) {
        }
        // First ordering fields too high
        try {
            union_Ordered(groupScan_Default(coi),
                          groupScan_Default(coi),
                          tXIndexRowType,
                          tXIndexRowType,
                          3,
                          1,
                          ascending(true),
                          false);
            fail();
        } catch (IllegalArgumentException e) {
        }
        // Second ordering fields too high
        try {
            union_Ordered(groupScan_Default(coi),
                          groupScan_Default(coi),
                          tXIndexRowType,
                          tXIndexRowType,
                          1,
                          3,
                          ascending(true),
                          false);
            fail();
        } catch (IllegalArgumentException e) {
        }
        // Different number of ordering fields
        try {
            union_Ordered(groupScan_Default(coi),
                          groupScan_Default(coi),
                          tXIndexRowType,
                          tXIndexRowType,
                          1,
                          2,
                          ascending(true),
                          false);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    // Runtime tests

    @Test
    public void testBothInputsEmpty()
    {
        Operator plan = unionPlan(0, 0, true, false);
        RowBase[] expected = new RowBase[] {
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
        plan = unionPlan(0, 0, false, false);
        expected = new RowBase[] {
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
    }

    @Test
    public void testLeftEmpty()
    {
        Operator plan = unionPlan(0, 1, true, false);
        RowBase[] expected = new RowBase[] {
            row(tRowType, 1L, 1000L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1002L),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
        plan = unionPlan(0, 1, false, false);
        expected = new RowBase[] {
            row(tRowType, 1L, 1002L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1000L),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
    }

    @Test
    public void testRightEmpty()
    {
        Operator plan = unionPlan(1, 0, true, false);
        RowBase[] expected = new RowBase[] {
            row(tRowType, 1L, 1000L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1002L),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
        plan = unionPlan(1, 0, false, false);
        expected = new RowBase[] {
            row(tRowType, 1L, 1002L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1000L),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
    }

    @Test
    public void testDuplicates()
    {
        Operator plan = unionPlan(1, 1, true, false);
        RowBase[] expected = new RowBase[] {
            row(tRowType, 1L, 1000L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1002L),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
        plan = unionPlan(1, 1, false, false);
        expected = new RowBase[] {
            row(tRowType, 1L, 1002L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1000L),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
        plan = unionPlan(1, 1, true, true);
        expected = new RowBase[] {
            row(tRowType, 1L, 1000L),
            row(tRowType, 1L, 1000L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1002L),
            row(tRowType, 1L, 1002L),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
    }

    @Test
    public void testDisjoint()
    {
        Operator plan = unionPlan(1, 2, true, false);
        RowBase[] expected = new RowBase[] {
            row(tRowType, 1L, 1000L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1002L),
            row(tRowType, 2L, 2000L),
            row(tRowType, 2L, 2001L),
            row(tRowType, 2L, 2002L),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
        plan = unionPlan(1, 2, false, false);
        expected = new RowBase[] {
            row(tRowType, 2L, 2002L),
            row(tRowType, 2L, 2001L),
            row(tRowType, 2L, 2000L),
            row(tRowType, 1L, 1002L),
            row(tRowType, 1L, 1001L),
            row(tRowType, 1L, 1000L),
        };
        compareRows(expected, cursor(plan, queryContext, queryBindings));
    }

    private Operator unionPlan(int k1, int k2, boolean ascending, boolean outputEqual)
    {
        Operator plan =
            union_Ordered(
                indexScan_Default(
                    tXIndexRowType,
                    parentXEq(k1),
                    ordering(field(tXIndexRowType, 1), ascending)),
                indexScan_Default(
                    tXIndexRowType,
                    parentXEq(k2),
                    ordering(field(tXIndexRowType, 1), ascending)),
                tXIndexRowType,
                tXIndexRowType,
                1,
                1,
                ascending(ascending),
                outputEqual);
        return plan;
    }

    private IndexKeyRange parentXEq(long x)
    {
        IndexBound xBound = new IndexBound(row(tXIndexRowType, x), new SetColumnSelector(0));
        return IndexKeyRange.bounded(tXIndexRowType, xBound, true, xBound, true);
    }

    private Ordering ordering(Object... objects)
    {
        Ordering ordering = API.ordering();
        int i = 0;
        while (i < objects.length) {
            ExpressionGenerator expression = (ExpressionGenerator) objects[i++];
            Boolean ascending = (Boolean) objects[i++];
            ordering.append(expression, ascending);
        }
        return ordering;
    }

    private boolean[] ascending(boolean... ascending)
    {
        return ascending;
    }
}
