package joyin.takgi.paysage.esim

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimSgp22SampleCollectionTemplateTest {
    @Test
    fun buildsTemplateWithDeclaredContextAndPrivacyChecklist() {
        val template = EsimSgp22SampleCollectionTemplateBuilder.build(
            EsimSgp22SampleContext.ProfileReadOnly
        )

        assertTrue(template.contains("Paysage SGP.22 安全样本采集模板"))
        assertTrue(template.contains("选择上下文: profile 只读"))
        assertTrue(template.contains("不外发原始 APDU"))
        assertTrue(template.contains("未包含 TLV value"))
        assertTrue(template.contains("未包含 EID/ICCID/IMSI/手机号/IMEI"))
        assertTrue(template.contains("脱敏分析片段"))
        assertFalse(template.contains("987654321000"))
    }

    @Test
    fun buildsTemplateForUnknownContext() {
        val template = EsimSgp22SampleCollectionTemplateBuilder.build(
            EsimSgp22SampleContext.Unknown
        )

        assertTrue(template.contains("选择上下文: 自动判断"))
        assertTrue(template.contains("实际 ES10x/诊断命令名"))
    }
}
