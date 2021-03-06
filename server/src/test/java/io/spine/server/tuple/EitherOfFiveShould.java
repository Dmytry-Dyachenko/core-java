/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.tuple;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import io.spine.test.TestValues;
import io.spine.time.Time;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("FieldNamingConvention") // short vars are OK for tuple tests.
public class EitherOfFiveShould {

    private final StringValue a = TestValues.newUuidValue();
    private final BoolValue b = BoolValue.of(true);
    private final Timestamp c = Time.getCurrentTime();
    private final UInt32Value d = UInt32Value.newBuilder()
                                             .setValue(512)
                                             .build();
    private final FloatValue e = FloatValue.newBuilder()
                                           .setValue(3.14159f)
                                           .build();

    private EitherOfFive<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithA;
    private EitherOfFive<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithB;
    private EitherOfFive<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithC;
    private EitherOfFive<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithD;
    private EitherOfFive<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithE;

    @Before
    public void setUp() {
        eitherWithA = EitherOfFive.withA(a);
        eitherWithB = EitherOfFive.withB(b);
        eitherWithC = EitherOfFive.withC(c);
        eitherWithD = EitherOfFive.withD(d);
        eitherWithE = EitherOfFive.withE(e);
    }

    @Test
    public void support_equality() {
        new EqualsTester().addEqualityGroup(eitherWithA, EitherOfFive.withA(a))
                          .addEqualityGroup(eitherWithB)
                          .addEqualityGroup(eitherWithC)
                          .addEqualityGroup(eitherWithD)
                          .addEqualityGroup(eitherWithE)
                          .testEquals();
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester().testAllPublicStaticMethods(EitherOfFive.class);
    }

    @Test
    public void return_values() {
        assertEquals(a, eitherWithA.getA());
        assertEquals(b, eitherWithB.getB());
        assertEquals(c, eitherWithC.getC());
        assertEquals(d, eitherWithD.getD());
        assertEquals(e, eitherWithE.getE());
    }

    @Test
    public void return_value_index() {
        assertEquals(0, eitherWithA.getIndex());
        assertEquals(1, eitherWithB.getIndex());
        assertEquals(2, eitherWithC.getIndex());
        assertEquals(3, eitherWithD.getIndex());
        assertEquals(4, eitherWithE.getIndex());
    }

    @Test
    public void return_only_one_value_in_iteration() {
        final Iterator<Message> iteratorA = eitherWithA.iterator();

        assertEquals(a, iteratorA.next());
        assertFalse(iteratorA.hasNext());

        final Iterator<Message> iteratorB = eitherWithB.iterator();

        assertEquals(b, iteratorB.next());
        assertFalse(iteratorB.hasNext());

        final Iterator<Message> iteratorC = eitherWithC.iterator();

        assertEquals(c, iteratorC.next());
        assertFalse(iteratorC.hasNext());

        final Iterator<Message> iteratorD = eitherWithD.iterator();

        assertEquals(d, iteratorD.next());
        assertFalse(iteratorD.hasNext());

        final Iterator<Message> iteratorE = eitherWithE.iterator();

        assertEquals(e, iteratorE.next());
        assertFalse(iteratorE.hasNext());
    }

    @Test
    public void serialize() {
        reserializeAndAssert(eitherWithA);
        reserializeAndAssert(eitherWithB);
        reserializeAndAssert(eitherWithC);
        reserializeAndAssert(eitherWithD);
        reserializeAndAssert(eitherWithE);
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_A_B() {
        eitherWithA.getB();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_A_C() {
        eitherWithA.getC();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_A_D() {
        eitherWithA.getD();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_A_E() {
        eitherWithA.getE();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_B_A() {
        eitherWithB.getA();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_B_C() {
        eitherWithB.getC();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_B_D() {
        eitherWithB.getD();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_B_E() {
        eitherWithB.getE();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_C_A() {
        eitherWithC.getA();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_C_B() {
        eitherWithC.getB();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_C_D() {
        eitherWithC.getD();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_C_E() {
        eitherWithC.getE();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_D_A() {
        eitherWithD.getA();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_D_B() {
        eitherWithD.getB();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_D_C() {
        eitherWithD.getC();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_D_E() {
        eitherWithD.getE();
    }
}
