package top.knos.mySeek.support;

import java.lang.reflect.ParameterizedType;

/**
 * TODO
 *
 * @Author andy
 * @Date 2022/3/13 21:22
 */
public class GenericType<T> {
    public T getInstanceOfT()
    {
        ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
        Class<T> type = (Class<T>) superClass.getActualTypeArguments()[0];
        try
        {
            return type.getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            // Oops, no default constructor
            throw new RuntimeException(e);
        }
    }

    public Class<T> getClassOfT()
    {
        ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
        Class<T> type = (Class<T>) superClass.getActualTypeArguments()[0];
        return type;
    }
}
