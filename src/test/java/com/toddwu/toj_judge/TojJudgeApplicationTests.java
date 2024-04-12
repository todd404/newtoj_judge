package com.toddwu.toj_judge;

import com.toddwu.toj_judge.pojo.ProblemConfig;
import com.toddwu.toj_judge.pojo.TypeMap;
import com.toddwu.toj_judge.utils.CppFileBuilder;
import com.toddwu.toj_judge.utils.RedisCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TojJudgeApplicationTests {
    @Autowired
    private TypeMap typeMap;
    @Autowired
    CppFileBuilder cppFileBuilder;

    @Autowired
    RedisCache redisCache;

//    @Test
//    void contextLoads() {
//        String code = """
//                class Solution {
//                public:
//                	vector<int> allAdd(vector<int> nums, int n) {
//                		for (int& num : nums) {
//                			num += n;
//                		}
//
//                		return nums;
//                	}
//                };
//
//                """;
//
//        ProblemConfig problemConfig = new ProblemConfig();
//        cppFileBuilder.buildCppFile(code, problemConfig);
//
//    }

//    @Test
//    void redisTest(){
//        String msg = redisCache.getCacheObject("judge:a4c4fca2-cfdf-4624-9ac9-388a7910d922");
//        return;
//    }

}
