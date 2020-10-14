package win.regin.sample.impl.ano;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author :Reginer in  2020/10/14 15:59.
 * 联系方式:QQ:282921012
 * 功能描述:参数注解
 */
@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface FuncParam {
    /**
     * 默认值
     *
     * @return 显示内容
     */
    String defaultValue() default "";

    /**
     * 参数说明
     *
     * @return -
     */
    String describe() default "";
}
