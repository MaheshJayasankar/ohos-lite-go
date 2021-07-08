/*
 * Copyright (C) 2020-21 Application Library Engineering Group
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

package com.litesuits.go.slice;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.ListContainer;
import com.litesuits.go.ListItemProvider;
import com.litesuits.go.ResourceTable;
import java.util.ArrayList;
import java.util.List;

/**
 * This ability slice displays a list of operations that may be performed by the LiteGo library.
 */
public class MainAbilitySlice extends AbilitySlice {

    private static final int LIST_ITEM_COUNT = 4;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_main);
        initViews();
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }

    private void initViews() {
        ListContainer listContainer = (ListContainer) findComponentById(ResourceTable.Id_list_container);
        List<Integer> list = getData();
        ListItemProvider listItemProvider = new ListItemProvider(list, this);
        listContainer.setItemProvider(listItemProvider);

        listContainer.setItemClickedListener((listContainer1, component, i, l) -> clickTestItem(i));
    }

    private List<Integer> getData() {
        List<Integer> list = new ArrayList<>();
        for (int idx = 0; idx < LIST_ITEM_COUNT; idx++) {
            list.add(idx);
        }
        return list;
    }

    /**
     * <item>0. Submit Runnable</item>
     * <item>1. Submit FutureTask</item>
     * <item>2. Submit Callable</item>
     * <item>3. Strategy Test</item>
     */
    private void clickTestItem(final int which) {
        Intent intent = new Intent();
        intent.setParam(TaskExecutorSlice.SELECTED_OPTION, which);
        present(new TaskExecutorSlice(), intent);
    }

}
