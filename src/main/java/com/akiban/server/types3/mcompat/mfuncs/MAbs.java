/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */
package com.akiban.server.types3.mcompat.mfuncs;

import com.akiban.server.types3.LazyList;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TOverload;
import com.akiban.server.types3.TOverloadResult;
import com.akiban.server.types3.common.funcs.Abs;
import com.akiban.server.types3.mcompat.mtypes.MBigDecimalWrapper;
import com.akiban.server.types3.mcompat.mtypes.MApproximateNumber;
import com.akiban.server.types3.mcompat.mtypes.MNumeric;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;

public class MAbs {

    public static final TOverload TINYINT = new Abs(MNumeric.TINYINT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt8((byte) Math.abs(inputs.get(0).getInt8()));
        }

        @Override
        public TOverloadResult resultType() {
            return TOverloadResult.fixed(MNumeric.INT.instance());
        }
    };
    public static final TOverload SMALLINT = new Abs(MNumeric.SMALLINT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt16((short) Math.abs(inputs.get(0).getInt16()));
        }

        @Override
        public TOverloadResult resultType() {
            return TOverloadResult.fixed(MNumeric.INT.instance());
        }
    };
    public static final TOverload MEDIUMINT = new Abs(MNumeric.MEDIUMINT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt32((int) Math.abs(inputs.get(0).getInt32()));
        }

        @Override
        public TOverloadResult resultType() {
            return TOverloadResult.fixed(MNumeric.INT.instance());
        }
    };
    public static final TOverload BIGINT = new Abs(MNumeric.BIGINT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt64((long) Math.abs(inputs.get(0).getInt64()));
        }
    };
    public static final TOverload INT = new Abs(MNumeric.INT) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putInt32((int) Math.abs(inputs.get(0).getInt32()));
        }
    };
    public static final TOverload DECIMAL = new Abs(MNumeric.DECIMAL) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            MBigDecimalWrapper wrapper = (MBigDecimalWrapper) inputs.get(0).getObject();
            output.putObject(wrapper.abs());
        }
    };
    public static final TOverload DOUBLE = new Abs(MApproximateNumber.DOUBLE) {

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            output.putDouble(Math.abs(inputs.get(0).getDouble()));
        }
    };
}
