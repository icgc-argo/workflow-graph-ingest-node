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

package org.icgc_argo.workflowingestionnode.components;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc_argo.workflow_graph_lib.schema.AnalysisFile;
import org.icgc_argo.workflow_graph_lib.schema.AnalysisSample;
import org.icgc_argo.workflow_graph_lib.schema.GraphEvent;
import org.icgc_argo.workflowingestionnode.model.GqlAnalysis;
import org.icgc_argo.workflowingestionnode.model.SongAnalysisEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@RequiredArgsConstructor
@Slf4j
public class MessageProcessor implements Function<Message<SongAnalysisEvent>, Message<GraphEvent>> {
  private static final String ACCEPTED_ANALYSIS_STATE = "PUBLISHED";
  private static final String ACCEPTED_ANALYSIS_TYPE = "sequencing_experiment";

  private final GqlAnalysesFetcher analysesFetcher;

  @SneakyThrows
  public Message<GraphEvent> apply(Message<SongAnalysisEvent> message) {
    // First filter messages that are not what we want
    val event = message.getPayload();
    log.info("Received SongAnalysisEvent: {}", event);
    if (!isAcceptedSongAnalysisEvent(event)) {
      log.info("Filtered (ignored) analysis: {}", event);
      return null; // returning null acknowledges and discards received message
    }
    val aid = event.getAnalysisId();
    val sid = event.getAnalysis().getStudyId();

    // Next check analysis in message exists in RDPC
    val gqlAnalysisOpt = analysesFetcher.apply(aid, sid);
    if (gqlAnalysisOpt.isEmpty()) {
      log.info(
          "GqlAnalysis could not be found for analysis in SongAnalysisEvent - {}, {}!", sid, aid);
      throw new Exception("GqlAnalysis could not be found for analysisId in SongAnalysisEvent!");
    }

    // Then check analysis state and type from RPDC is what we found in the event
    val gqlAnalysis = gqlAnalysisOpt.get();
    if (!isAcceptedGqlAnalysis(gqlAnalysis)) {
      log.info(
          "GqlAnalysis analysis state and type is different from SongAnalysisEvent - {}, {}!",
          sid,
          aid);
      throw new Exception(
          "GqlAnalysis analysis state and type is different from SongAnalysisEvent!");
    }

    // After all checks are good, create GraphEvent and return
    val ge = convertToGraphEvent(gqlAnalysis);
    log.info("Created graph event: {}", ge);
    return convertToGraphEventMessage(ge);
  }

  private Message<GraphEvent> convertToGraphEventMessage(GraphEvent graphEvent) {
    return MessageBuilder.withPayload(graphEvent)
        .setHeader("contentType", "application/vnd.GraphEvent+avro")
        .build();
  }

  private Boolean isAcceptedSongAnalysisEvent(SongAnalysisEvent event) {
    return event.getAnalysis().getAnalysisType().getName().equalsIgnoreCase(ACCEPTED_ANALYSIS_TYPE)
        && event.getAnalysis().getAnalysisState().equalsIgnoreCase(ACCEPTED_ANALYSIS_STATE);
  }

  private Boolean isAcceptedGqlAnalysis(GqlAnalysis analysis) {
    return analysis.getAnalysisType().equalsIgnoreCase(ACCEPTED_ANALYSIS_TYPE)
        && analysis.getAnalysisState().equalsIgnoreCase(ACCEPTED_ANALYSIS_STATE);
  }

  private GraphEvent convertToGraphEvent(GqlAnalysis analysis) {
    return GraphEvent.newBuilder()
        .setId(UUID.randomUUID().toString())
        .setAnalysisId(analysis.getAnalysisId())
        .setAnalysisState(analysis.getAnalysisState())
        .setAnalysisType(analysis.getAnalysisType())
        .setStudyId(analysis.getStudyId())
        .setSamples(createAnalysisSamplesForGe(analysis))
        .setFiles(createAnalysisFilesForGe(analysis))
        .setExperimentalStrategy(analysis.getExperiment().getExperimentalStrategy())
        .build();
  }

  private List<AnalysisSample> createAnalysisSamplesForGe(GqlAnalysis analysis) {
    return analysis.getDonors().stream()
        .flatMap(
            donor ->
                donor.getSpecimens().stream()
                    .flatMap(
                        specimen ->
                            specimen.getSamples().stream()
                                .map(
                                    sample ->
                                        AnalysisSample.newBuilder()
                                            .setDonorId(donor.getDonorId())
                                            .setSubmitterDonorId(donor.getSubmitterDonorId())
                                            .setSpecimenId(specimen.getSpecimenId())
                                            .setSubmitterSpecimenId(
                                                specimen.getSubmitterSpecimenId())
                                            .setTumourNormalDesignation(
                                                specimen.getTumourNormalDesignation())
                                            .setSampleId(sample.getSampleId())
                                            .setSubmitterSampleId(sample.getSubmitterSampleId())
                                            .build())))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<AnalysisFile> createAnalysisFilesForGe(GqlAnalysis analysis) {
    return analysis.getFiles().stream()
        .map(f -> new AnalysisFile(f.getDataType()))
        .collect(toUnmodifiableList());
  }
}
