package win.regin.sample.impl.ano;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author :Reginer in  2020/10/14 15:59.
 * 联系方式:QQ:282921012
 * 功能描述:方法注解
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FuncMethod {
    /**
     * 方法名
     *
     * @return -
     */
    String name();
}
