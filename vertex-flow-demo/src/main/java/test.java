import com.google.common.collect.Lists;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class test {


    public static void main(String[] args) {

        Optional.ofNullable(args[0])
                .map(String::toLowerCase)
                .or(() -> Optional.ofNullable(args[1]))
                .ifPresent(System.out::println);


    }
}
