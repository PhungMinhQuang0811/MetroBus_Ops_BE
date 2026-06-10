package com.vdt.afc_ops_service.common.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchFilterUtilTest {

    @Test
    void normalize_HandlesNullBlankAndTrimmedValues() {
        assertNull(SearchFilterUtil.normalize(null));
        assertNull(SearchFilterUtil.normalize("   "));
        assertEquals("metro", SearchFilterUtil.normalize(" metro "));
    }

    @Test
    void normalizeUppercase_HandlesNullAndValues() {
        assertNull(SearchFilterUtil.normalizeUppercase(null));
        assertEquals("METRO", SearchFilterUtil.normalizeUppercase(" metro "));
    }

    @Test
    void toKeywordPattern_HandlesNullAndValues() {
        assertNull(SearchFilterUtil.toKeywordPattern("   "));
        assertEquals("%metro%", SearchFilterUtil.toKeywordPattern(" Metro "));
    }

    @Test
    void constructor_IsPrivateUtilityConstructor() throws Exception {
        Constructor<SearchFilterUtil> constructor = SearchFilterUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        SearchFilterUtil instance = constructor.newInstance();

        assertTrue(instance instanceof SearchFilterUtil);
    }
}
