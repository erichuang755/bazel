// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.android.ziputils;

import static com.google.devtools.build.android.ziputils.DirectoryEntry.CENTIM;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Unit tests for {@link CentralDirectory}.
 */
@RunWith(JUnit4.class)
public class CentralDirectoryTest {

  /**
   * Test of viewOf method, of class CentralDirectory.
   */
  @Test
  public void testViewOf() {
    ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < 100; i++) {
      buffer.put((byte) i);
    }
    buffer.position(20);
    buffer.limit(90);
    CentralDirectory view = CentralDirectory.viewOf(buffer);
    int expPos = 0;
    int expLimit = 90;
    // expect the buffer to have been reset to 0 (CentralDirectory does NOT slice).
    assertEquals("View not at position 0", expPos, view.buffer.position());
    assertEquals("Buffer not at position 0", expPos, buffer.position());
    assertEquals("Buffer limit changed", expLimit, view.buffer.limit());
    assertEquals("Buffer limit changed", expLimit, buffer.limit());
  }

  /**
   * Test of parse method, of class CentralDirectory.
   */
  @Test
  public void testParse() {
    // First fill it with some entries
    ByteBuffer inputBuffer = ByteBuffer.allocate(10000).order(ByteOrder.LITTLE_ENDIAN);
    String comment = null;
    byte[] extra = null;
    String filename = "pkg/0.txt";
    DirectoryEntry entry = DirectoryEntry.view(inputBuffer, filename, extra , comment);
    int expSize = entry.getSize();
    comment = "";
    extra = new byte[]{};
    for (int i = 1; i < 20; i++) {
      filename = "pkg/" + i + ".txt";
      entry = DirectoryEntry.view(inputBuffer, filename, extra , comment);
      expSize += entry.getSize();
      extra = new byte[extra.length + 1];
      comment = comment + "," + i;
    }
    // Parse the entries.
    CentralDirectory cdir = CentralDirectory.viewOf(inputBuffer).at(0).parse();
    assertEquals("Count", 20, cdir.getCount());
    assertEquals("Position after parse", expSize, cdir.buffer.position());
    assertEquals("Limit after parse", 10000, cdir.buffer.limit());
    cdir.buffer.flip();
    assertEquals("Position after finish", 0, cdir.buffer.position());
    assertEquals("Limit after finish", expSize, cdir.buffer.limit());
  }

  /**
   * Test of nextEntry method, of class CentralDirectory.
   */
  @Test
  public void testNextEntry() {
    ByteBuffer outputBuffer = ByteBuffer.allocate(10000).order(ByteOrder.LITTLE_ENDIAN);
    CentralDirectory cdir = CentralDirectory.viewOf(outputBuffer);
    String comment = null;
    byte[] extra = null;
    String filename = "pkg/0.txt";
    DirectoryEntry entry = DirectoryEntry.allocate(filename, extra , comment);
    cdir.nextEntry(entry).set(CENTIM, 0);
    int expSize = entry.getSize();
    comment = "";
    extra = new byte[]{};
    for (int i = 1; i < 20; i++) {
      filename = "pkg/" + i + ".txt";
      entry = DirectoryEntry.allocate(filename, extra , comment);
      cdir.nextEntry(entry).set(CENTIM, 0);
      int size = entry.getSize();
      expSize += size;
      extra = new byte[extra.length + 1];
      comment = comment + "," + i;
    }
    assertEquals("Count", 20, cdir.getCount());
    assertEquals("Position after build", expSize, cdir.buffer.position());
    assertEquals("Limit after build", 10000, cdir.buffer.limit());
    cdir.buffer.flip();
    assertEquals("Position after finish build", 0, cdir.buffer.position());
    assertEquals("Limit after finish build", expSize, cdir.buffer.limit());

    // now try to parse the directory we just created.
    cdir.at(0).parse();
    assertEquals("Count", 20, cdir.getCount());
    assertEquals("Position after re-parse", expSize, cdir.buffer.position());
    assertEquals("Limit after re-parse", expSize, cdir.buffer.limit());
    cdir.buffer.flip();
    assertEquals("Position after finish parse", 0, cdir.buffer.position());
    assertEquals("Limit after finish parse", expSize, cdir.buffer.limit());
  }
}
