import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public class Infix {
    /**
     * 计算中缀表达式的求值结果。
     * <p>
     * 测试的表达式总会得出有效数值且不存在除数为 0 的情况。
     *
     * @param tokens
     * @return 中缀表达式的求值结果。
     */
    Map<String, Integer> map = new HashMap<String, Integer>() {
        {
            put("-", 1);
            put("+", 1);
            put("*", 2);
            put("/", 2);
        }
    };

    public float cal(float x, float y, String op) {
        if (Objects.equals(op, "+")) return x + y;
        if (Objects.equals(op, "-")) return x - y;
        if (Objects.equals(op, "*")) return x * y;
        if (Objects.equals(op, "/")) return x / y;
        return 0;
    }

    public float infix(String[] tokens) {
        float ans = 0;
        //创造numbers和ops两种队列
        /*
        前面准备，number数组。符号和优先级的map，op对应的操作的函数
        规则：
            遍历吗tokens的每一个，
            1. (进队
            2. )从nums里面拿2个，从ops里面拿一个，然后计算，结果放到number里面，直到遇到((具体实现要保证ops不为空)
            3. map.containsKey(token),key是一个符号，然后比较这个符号和ops的符号的优先级关系，把前面的优先级高的都计算了，然后再把自己放进数组里面
                3.1
                    这个的处理比较巧妙，首先保证ops的栈不为空，然后保证ops的栈顶元素不是（，然后再去讨论优先级，只要后面的优先级比前面的高就要进行计算，直到最后计算完成。
            4.如果不是就直接放进数组里面
            5.如果是数字直接放进数组里面
         */
        Stack<Float> numbers = new Stack<Float>();
        Stack<String> ops = new Stack<String>();
        for (String token : tokens) {
            if (token.equals("(")) {
                ops.add(token);
            } else if (token.equals(")")) {
                while (!ops.empty()) {
                    if (!ops.peek().equals("(")) {
                        float x = numbers.pop();
                        float y = numbers.pop();
                        String op = ops.pop();
                        ans= cal(y, x, op);
                        numbers.add(ans);
                        System.out.println(y+op+x+"="+ans);
                    } else {
                        ops.pop();
                        break;
                    }
                }
            } else if (map.containsKey(token)) {
                while (!ops.empty() && !ops.peek().equals("(")) {
                    String prev = ops.peek();
                    if (map.get(prev) >= map.get(token)) {
                        float x = numbers.pop();
                        float y = numbers.pop();
                        String op = ops.pop();
                        ans= cal(y, x, op);
                        numbers.add(ans);
                        System.out.println(y+op+x+"="+ans);
                    } else {
                        break;
                    }
                }
                ops.add(token);
            } else {
                numbers.add(Float.valueOf(token));
            }


        }
        while (!ops.empty()) {
                float x = numbers.pop();
                float y = numbers.pop();
                String op = ops.pop();
                ans = cal(y, x, op);
                numbers.add(ans);
                System.out.println(y + op + x + "=" + ans);
        }
            return ans;
    }
}