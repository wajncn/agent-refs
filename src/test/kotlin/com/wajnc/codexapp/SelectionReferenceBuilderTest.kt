package com.wajnc.codexapp

import com.intellij.openapi.editor.impl.DocumentImpl
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SelectionReferenceBuilderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `builds a multi-line project-relative reference`() {
        val project = temporaryFolder.newFolder("broker-trading-svc").toPath()
        val file = project.resolve(
            "src/main/java/com/innodealing/brokertrading/service/impl/ApprovalTradeServiceImpl.java",
        )

        val reference = SelectionReferenceBuilder.build(
            project.toString(),
            file.toString(),
            53,
            54,
        )

        assertEquals(
            "@src\\main\\java\\com\\innodealing\\brokertrading\\service\\impl\\" +
                "ApprovalTradeServiceImpl.java#L53-54",
            reference,
        )
    }

    @Test
    fun `uses a compact suffix for a single line`() {
        val project = temporaryFolder.newFolder("sample-project").toPath()
        val file = project.resolve("src/main/java/Example.java")

        val reference = SelectionReferenceBuilder.build(project.toString(), file.toString(), 8, 8)

        assertEquals("@src\\main\\java\\Example.java#L8", reference)
    }

    @Test
    fun `builds a mac reference with forward slashes`() {
        assertEquals(
            "@src/main/Example.java#L10-12",
            SelectionReferenceBuilder.build("/work/project", "/work/project/src/main/Example.java", 10, 12, "/"),
        )
    }

    @Test
    fun `builds a reference from a selected document range`() {
        val project = temporaryFolder.newFolder("sample-project").toPath()
        val file = project.resolve("src/Example.java")
        val document = DocumentImpl("first line\nsecond line\nthird line")

        val reference = SelectionReferenceBuilder.buildSelection(
            project.toString(),
            file.toString(),
            document,
            selectionStart = 2,
            selectionEnd = 24,
        )

        assertEquals("@src\\Example.java#L1-3", reference)
    }
}
