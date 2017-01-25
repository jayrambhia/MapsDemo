package com.fenchtose.mapsdemo.db;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by Jay Rambhia on 8/2/16.
 */
public interface AbstractDao<T> {

    int add(@NonNull T t);

    @Nullable
    T get(int id);

    List<T> getAll();

    int getCount();

    int update(@NonNull T t);

    int delete(@NonNull T t);
}
