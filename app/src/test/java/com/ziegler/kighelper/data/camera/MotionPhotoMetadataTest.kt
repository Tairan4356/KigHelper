package com.ziegler.kighelper.data.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionPhotoMetadataTest {

    @Test
    fun readsContainerVideoLength() {
        val xmp = """
            <rdf:Description GCamera:MotionPhoto="1">
             <Container:Directory>
              <rdf:Seq>
               <rdf:li rdf:parseType="Resource">
                <Container:Item Item:Mime="image/jpeg" Item:Semantic="Primary" Item:Length="0"/>
               </rdf:li>
               <rdf:li rdf:parseType="Resource">
                <Container:Item Item:Length="2048" Item:Mime="video/mp4" Item:Semantic="MotionPhoto"/>
               </rdf:li>
              </rdf:Seq>
             </Container:Directory>
            </rdf:Description>
        """.trimIndent()

        assertEquals(2048L, MotionPhotoMetadata.videoLength(xmp, fileSize = 10_000L))
    }

    @Test
    fun readsLegacyMicroVideoOffset() {
        val xmp = """
            <rdf:Description
              xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
              GCamera:MicroVideo="1"
              GCamera:MicroVideoOffset="4096"/>
        """.trimIndent()

        assertEquals(4096L, MotionPhotoMetadata.videoLength(xmp, fileSize = 20_000L))
    }

    @Test
    fun prefersContainerLengthOverLegacyOffset() {
        val xmp = """
            <rdf:Description GCamera:MicroVideoOffset="100">
             <Container:Item Item:Semantic="MotionPhoto" Item:Length="3000" Item:Mime="video/mp4"/>
            </rdf:Description>
        """.trimIndent()

        assertEquals(3000L, MotionPhotoMetadata.videoLength(xmp, fileSize = 20_000L))
    }

    @Test
    fun rejectsMissingOrInvalidLength() {
        assertNull(MotionPhotoMetadata.videoLength(null, 10_000L))
        assertNull(MotionPhotoMetadata.videoLength("", 10_000L))
        assertNull(MotionPhotoMetadata.videoLength("<x/>", 10_000L))
        assertNull(
            MotionPhotoMetadata.videoLength(
                "<Container:Item Item:Semantic=\"MotionPhoto\" Item:Length=\"12000\"/>",
                fileSize = 10_000L
            )
        )
        assertNull(
            MotionPhotoMetadata.videoLength(
                "<Container:Item Item:Semantic=\"MotionPhoto\" Item:Length=\"0\"/>",
                fileSize = 10_000L
            )
        )
    }
}
