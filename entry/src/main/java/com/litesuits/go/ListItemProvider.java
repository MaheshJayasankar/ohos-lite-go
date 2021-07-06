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

package com.litesuits.go;

import ohos.aafwk.ability.AbilitySlice;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import java.util.Arrays;
import java.util.List;

/**
 * A simple implementation of the BaseItemProvider class to handle 4 list objects holding string values.
 */
public class ListItemProvider extends BaseItemProvider {

    private static final int ITEM_COUNT = 4;
    private final List<String> itemNames = Arrays.asList("Submit Runnable",
            "Submit FutureTask",
            "Submit Callable",
            "Strategy Test",
            "null");



    private final List<Integer> list;
    private final AbilitySlice slice;

    public ListItemProvider(List<Integer> list, AbilitySlice slice) {
        this.list = list;
        this.slice = slice;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < ITEM_COUNT) {
            return list.get(position);
        } else {
            return list.get(list.size() - 1);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Component getComponent(int position, Component convertComponent, ComponentContainer componentContainer) {
        final Component cpt;
        if (convertComponent == null) {
            cpt = LayoutScatter.getInstance(slice).parse(ResourceTable.Layout_list_item, null, false);
        } else {
            cpt = convertComponent;
        }
        int item = list.get(position);
        Text text = (Text) cpt.findComponentById(ResourceTable.Id_tv_item);
        text.setText(itemNames.get(item));
        return cpt;
    }
}
