package com.where.place;

public class StorablePlace <Storage extends StorageStrategy>extends Place
{
    Storage storage_;
    public StorablePlace(Storage storage)
    {
        super();
        storage_ = storage;
    }
}
