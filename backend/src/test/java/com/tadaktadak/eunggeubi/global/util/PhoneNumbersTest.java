package com.tadaktadak.eunggeubi.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void 로그용으로_가운데_자리를_가린다() {
        assertThat(PhoneNumbers.mask("01012345678")).isEqualTo("010****5678");
        assertThat(PhoneNumbers.mask("010-123-4567")).isEqualTo("010***4567");
    }

    @Test
    void 짧거나_없는_번호는_전부_가린다() {
        assertThat(PhoneNumbers.mask("1234")).isEqualTo("***");
        assertThat(PhoneNumbers.mask(null)).isEqualTo("***");
    }
}
