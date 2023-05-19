/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc_argo.workflowingestionnode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// This data class is a sub class of AnalysisMessage in SONG server:
// https://github.com/overture-stack/SONG/blob/develop/song-server/src/main/java/bio/overture/song/server/kafka/AnalysisMessage.java

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SongAnalysisEvent {
  private String analysisId;
  private Analysis analysis;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Analysis {
    private String analysisId;
    private AnalysisType analysisType;
    private String analysisState;
    private String studyId;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AnalysisType {
    private String name;
  }
}
