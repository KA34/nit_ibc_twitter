package com.ka34.nit_ibc_twitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//休講情報更新時のデータ格納場所
public class ListData {
    public static String infoData;
    public static String makeData;
    public List<Map<String,String>> parseList = new ArrayList<>();
    public List<Map<String,String>> deleteList = new ArrayList<>();

    public List<Map<String,String>> matchList = new ArrayList<>();
    public Map<String,List<String>> TweetData = new HashMap<>();

    public List<Map<String,String>> GetfilterList(String grade, String dep, String clas){
        List<Map<String,String>> filterList;
        filterList = new ArrayList<>();
        List<String> filter = new ArrayList<>();
        filter.add(grade+dep);
        filter.add(grade+"年");
        filter.add("全学年");
        if(clas!=null){
            filter.add(grade+"の"+clas);
        }

        for (int i = 0; i < parseList.size(); i++) {
            Map<String, String> tmpMap;
            tmpMap = parseList.get(i);
            if (!tmpMap.get("type").equals("other")) {
                String tmpclas = tmpMap.get("clas");
                for (int j = 0; j < filter.size(); j++) {
                    if (tmpclas.equals(filter.get(j))) {
                        filterList.add(tmpMap);
                    }
                }
            }
        }

        Collections.sort(filterList, new Comparator<Map<String, String>>() {
            public int compare(Map<String, String> map1, Map<String, String> map2) {

                String S1 = map1.get("compare");
                String S2 = map2.get("compare");

                return S1.compareTo(S2);
            }
        });
        return filterList;
    }
    public List<Map<String,String>> GetdeletefilterList(String grade, String dep, String clas){
        List<Map<String,String>> filterList;
        filterList = new ArrayList<>();
        List<String> filter = new ArrayList<>();
        filter.add(grade+dep);
        filter.add(grade+"年");
        filter.add("全学年");
        if(clas!=null){
            filter.add(grade+"の"+clas);
        }

        for (int i = 0; i < deleteList.size(); i++) {
            Map<String, String> tmpMap;
            tmpMap = deleteList.get(i);
            if (!tmpMap.get("type").equals("other")) {
                String tmpclas = tmpMap.get("clas");
                for (int j = 0; j < filter.size(); j++) {
                    if (tmpclas.equals(filter.get(j))) {
                        filterList.add(tmpMap);
                    }
                }
            }
        }

        Collections.sort(filterList, new Comparator<Map<String, String>>() {
            public int compare(Map<String, String> map1, Map<String, String> map2) {

                String S1 = map1.get("compare");
                String S2 = map2.get("compare");

                return S1.compareTo(S2);
            }
        });
        return filterList;
    }
}