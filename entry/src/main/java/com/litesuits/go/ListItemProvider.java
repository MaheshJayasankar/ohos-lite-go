package com.litesuits.go;

import java.util.Arrays;
import java.util.List;
import ohos.aafwk.ability.AbilitySlice;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;

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
