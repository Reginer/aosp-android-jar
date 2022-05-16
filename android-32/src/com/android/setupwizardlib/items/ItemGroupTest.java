/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.setupwizardlib.items;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK, Config.NEWEST_SDK})
public class ItemGroupTest {

  private static final Item CHILD_1 = new EqualsItem("Child 1");
  private static final Item CHILD_2 = new EqualsItem("Child 2");
  private static final Item CHILD_3 = new EqualsItem("Child 3");
  private static final Item CHILD_4 = new EqualsItem("Child 4");

  private ItemGroup itemGroup;

  @Mock private ItemHierarchy.Observer observer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    itemGroup = new ItemGroup();
    itemGroup.registerObserver(observer);
  }

  @Test
  public void testGroup() {
    itemGroup.addChild(CHILD_1);
    itemGroup.addChild(CHILD_2);

    assertWithMessage("Item at position 0 should be child1")
        .that(itemGroup.getItemAt(0))
        .isSameAs(CHILD_1);
    assertWithMessage("Item at position 1 should be child2")
        .that(itemGroup.getItemAt(1))
        .isSameAs(CHILD_2);
    assertWithMessage("Should have 2 children").that(itemGroup.getCount()).isEqualTo(2);

    final InOrder inOrder = inOrder(observer);
    inOrder.verify(observer).onItemRangeInserted(eq(itemGroup), eq(0), eq(1));
    inOrder.verify(observer).onItemRangeInserted(eq(itemGroup), eq(1), eq(1));
  }

  @Test
  public void testRemoveChild() {
    itemGroup.addChild(CHILD_1);
    itemGroup.addChild(CHILD_2);
    itemGroup.addChild(CHILD_3);

    itemGroup.removeChild(CHILD_2);

    assertWithMessage("Item at position 0 should be child1")
        .that(itemGroup.getItemAt(0))
        .isSameAs(CHILD_1);
    assertWithMessage("Item at position 1 should be child3")
        .that(itemGroup.getItemAt(1))
        .isSameAs(CHILD_3);
    assertWithMessage("Should have 2 children").that(itemGroup.getCount()).isEqualTo(2);

    verify(observer).onItemRangeRemoved(eq(itemGroup), eq(1), eq(1));
  }

  @Test
  public void testClear() {
    itemGroup.addChild(CHILD_1);
    itemGroup.addChild(CHILD_2);

    itemGroup.clear();

    assertWithMessage("Should have 0 child").that(itemGroup.getCount()).isEqualTo(0);

    verify(observer).onItemRangeRemoved(eq(itemGroup), eq(0), eq(2));
  }

  @Test
  public void testNestedGroup() {
    ItemGroup parentGroup = new ItemGroup();
    ItemGroup childGroup = new ItemGroup();
    parentGroup.registerObserver(observer);

    parentGroup.addChild(CHILD_1);
    childGroup.addChild(CHILD_2);
    childGroup.addChild(CHILD_3);
    parentGroup.addChild(childGroup);
    parentGroup.addChild(CHILD_4);

    assertWithMessage("Position 0 should be child 1")
        .that(parentGroup.getItemAt(0))
        .isSameAs(CHILD_1);
    assertWithMessage("Position 1 should be child 2")
        .that(parentGroup.getItemAt(1))
        .isSameAs(CHILD_2);
    assertWithMessage("Position 2 should be child 3")
        .that(parentGroup.getItemAt(2))
        .isSameAs(CHILD_3);
    assertWithMessage("Position 3 should be child 4")
        .that(parentGroup.getItemAt(3))
        .isSameAs(CHILD_4);

    final InOrder inOrder = inOrder(observer);
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(0), eq(1));
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(1), eq(2));
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(3), eq(1));
    verifyNoMoreInteractions(observer);
  }

  @Test
  public void testNestedGroupClearNotification() {
    ItemGroup parentGroup = new ItemGroup();
    ItemGroup childGroup = new ItemGroup();
    parentGroup.registerObserver(observer);

    parentGroup.addChild(CHILD_1);
    childGroup.addChild(CHILD_2);
    childGroup.addChild(CHILD_3);
    parentGroup.addChild(childGroup);
    parentGroup.addChild(CHILD_4);

    childGroup.clear();

    final InOrder inOrder = inOrder(observer);
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(0), eq(1));
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(1), eq(2));
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(3), eq(1));
    verify(observer).onItemRangeRemoved(eq(parentGroup), eq(1), eq(2));
    verifyNoMoreInteractions(observer);
  }

  @Test
  public void testNestedGroupRemoveNotification() {
    ItemGroup parentGroup = new ItemGroup();
    ItemGroup childGroup = new ItemGroup();
    parentGroup.registerObserver(observer);

    parentGroup.addChild(CHILD_1);
    childGroup.addChild(CHILD_2);
    childGroup.addChild(CHILD_3);
    parentGroup.addChild(childGroup);
    parentGroup.addChild(CHILD_4);

    childGroup.removeChild(CHILD_3);
    childGroup.removeChild(CHILD_2);

    final InOrder inOrder = inOrder(observer);
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(0), eq(1));
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(1), eq(2));
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(3), eq(1));
    inOrder.verify(observer).onItemRangeRemoved(eq(parentGroup), eq(2), eq(1));
    inOrder.verify(observer).onItemRangeRemoved(eq(parentGroup), eq(1), eq(1));
    verifyNoMoreInteractions(observer);
  }

  @Test
  public void testNestedGroupClear() {
    ItemGroup parentGroup = new ItemGroup();
    ItemGroup childGroup = new ItemGroup();
    parentGroup.registerObserver(observer);

    parentGroup.addChild(CHILD_1);
    childGroup.addChild(CHILD_2);
    childGroup.addChild(CHILD_3);
    parentGroup.addChild(childGroup);

    childGroup.clear();

    final InOrder inOrder = inOrder(observer);
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(0), eq(1));
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(1), eq(2));
    inOrder.verify(observer).onItemRangeRemoved(eq(parentGroup), eq(1), eq(2));
    verifyNoMoreInteractions(observer);
  }

  @Test
  public void testNestedGroupRemoveLastChild() {
    ItemGroup parentGroup = new ItemGroup();
    ItemGroup childGroup1 = new ItemGroup();
    ItemGroup childGroup2 = new ItemGroup();
    parentGroup.registerObserver(observer);

    childGroup1.addChild(CHILD_1);
    childGroup1.addChild(CHILD_2);
    parentGroup.addChild(childGroup1);
    childGroup2.addChild(CHILD_3);
    childGroup2.addChild(CHILD_4);
    parentGroup.addChild(childGroup2);

    childGroup2.removeChild(CHILD_4);
    childGroup2.removeChild(CHILD_3);

    final InOrder inOrder = inOrder(observer);
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(0), eq(2));
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(2), eq(2));
    inOrder.verify(observer).onItemRangeRemoved(eq(parentGroup), eq(3), eq(1));
    inOrder.verify(observer).onItemRangeRemoved(eq(parentGroup), eq(2), eq(1));
    verifyNoMoreInteractions(observer);
  }

  @Test
  public void testNestedGroupClearOnlyChild() {
    ItemGroup parentGroup = new ItemGroup();
    ItemGroup childGroup = new ItemGroup();
    parentGroup.registerObserver(observer);

    childGroup.addChild(CHILD_1);
    childGroup.addChild(CHILD_2);
    parentGroup.addChild(childGroup);

    childGroup.clear();

    final InOrder inOrder = inOrder(observer);
    inOrder.verify(observer).onItemRangeInserted(eq(parentGroup), eq(0), eq(2));
    inOrder.verify(observer).onItemRangeRemoved(eq(parentGroup), eq(0), eq(2));
    verifyNoMoreInteractions(observer);
  }

  @Test
  public void testNotifyChange() {
    itemGroup.addChild(CHILD_1);
    itemGroup.addChild(CHILD_2);

    CHILD_2.setTitle("Child 2 modified");

    verify(observer).onItemRangeChanged(eq(itemGroup), eq(1), eq(1));
  }

  @Test
  public void testEmptyChildGroup() {
    ItemGroup parentGroup = new ItemGroup();
    ItemGroup childGroup = new ItemGroup();

    parentGroup.addChild(CHILD_1);
    parentGroup.addChild(childGroup);
    parentGroup.addChild(CHILD_2);

    assertWithMessage("Position 0 should be child 1")
        .that(parentGroup.getItemAt(0))
        .isSameAs(CHILD_1);
    assertWithMessage("Position 1 should be child 2")
        .that(parentGroup.getItemAt(1))
        .isSameAs(CHILD_2);
  }

  @Test
  public void testFindItemById() {
    CHILD_1.setId(12345);
    CHILD_2.setId(23456);

    itemGroup.addChild(CHILD_1);
    itemGroup.addChild(CHILD_2);

    assertWithMessage("Find item 23456 should return child 2")
        .that(itemGroup.findItemById(23456))
        .isSameAs(CHILD_2);
  }

  @Test
  public void testFindItemByIdNotFound() {
    CHILD_1.setId(12345);
    CHILD_2.setId(23456);

    itemGroup.addChild(CHILD_1);
    itemGroup.addChild(CHILD_2);

    assertWithMessage("ID not found should return null")
        .that(itemGroup.findItemById(56789))
        .isNull();
  }

  /**
   * This class will always return true on {@link #equals(Object)}. Used to ensure that ItemGroup is
   * using identity rather than equals(). Be sure to use assertSame rather than assertEquals when
   * comparing items of this class.
   */
  private static class EqualsItem extends Item {

    EqualsItem(String name) {
      setTitle(name);
    }

    @Override
    public int hashCode() {
      return 1;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Item;
    }

    @Override
    public String toString() {
      return "EqualsItem{title=" + getTitle() + "}";
    }
  }
}
