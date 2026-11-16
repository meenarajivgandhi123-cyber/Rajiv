package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BatchStatus
import com.example.data.model.ClaimStatus
import com.example.data.model.FarmStatus
import com.example.data.model.Farmer
import com.example.data.model.FeedDispatch
import com.example.data.model.FlockBatch
import com.example.data.model.IntegrationZone
import com.example.data.model.KmClaim
import com.example.ui.components.BatchStatusBadge
import com.example.ui.components.CloudSyncPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDecimal
import com.example.ui.components.formatNumber
import com.example.ui.theme.LocalStatusColors
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.viewmodel.FarmFilter

@Composable
fun OverviewScreen(
  zones: List<IntegrationZone>,
  farmers: List<Farmer>,
  batches: List<FlockBatch>,
  feedDispatches: List<FeedDispatch>,
  kmClaims: List<KmClaim>,
  todayEpochDay: Long,
  onOpenFarmsFilter: (FarmFilter) -> Unit,
  onOpenBatchDetail: (String) -> Unit,
  onCreateZoneClick: () -> Unit,
  onCreateFarmerClick: () -> Unit,
  onCreateBatchClick: () -> Unit,
  onOpenZoneReport: () -> Unit,
  onOpenKmClaims: () -> Unit,
  onFeedDispatchClick: () -> Unit = {},
  onBirdSaleClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val activeFarms = farmers.count { it.statusEnum == FarmStatus.ACTIVE }
  val closingFarms = farmers.count { it.statusEnum == FarmStatus.CLOSING }
  val emptyFarms = farmers.count { it.statusEnum == FarmStatus.EMPTY }
  val cancelledFarms = farmers.count { it.statusEnum == FarmStatus.CANCELLED }

  val openBatches = batches.filter { it.statusEnum != BatchStatus.CLOSED }
  val closedBatches = batches.filter { it.statusEnum == BatchStatus.CLOSED }
  val totalLiveBirds = openBatches.sumOf { it.survivingBirds }
  val totalFeedKg = batches.sumOf { it.netFeedConsumedKg }
  val fcrValues = batches.mapNotNull { it.effectiveFcr.takeIf { f -> f > 0.2 } }
  val avgFcr = if (fcrValues.isNotEmpty()) fcrValues.average() else 0.0
  val totalSettled = closedBatches.sumOf { it.settlementPayout }
  val totalDispatchedBags = feedDispatches.sumOf { it.bagsCount }
  val pendingKmClaims = kmClaims.count { it.statusEnum == ClaimStatus.PENDING }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWide = maxWidth >= 720.dp
    val statusColors = LocalStatusColors.current

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero Banner
      item {
        Card(
          shape = MaterialTheme.shapes.extraLarge,
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(if (isWide) 200.dp else 185.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_broiler_hero),
              contentDescription = "SPF Poultry Integration overview banner",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(
                      Color(0xFF0E2B22).copy(alpha = 0.90f),
                      Color(0xFF0E2B22).copy(alpha = 0.55f),
                      Color(0xFF0E2B22).copy(alpha = 0.20f),
                    )
                  )
                )
            )
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              CloudSyncPill()
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "SPF Poultry Integration Hub",
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White,
                )
                Text(
                  text = "${zones.size} Zones • ${farmers.size} Farms • ${openBatches.size} Live Batches • ${closedBatches.size} GC Closed",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White.copy(alpha = 0.9f),
                )
              }
            }
          }
        }
      }

      if (pendingKmClaims > 0) {
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = statusColors.closingBg,
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .clickable(onClick = onOpenKmClaims)
              .testTag("overview_pending_km_banner"),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = statusColors.closingFg)
                Column {
                  Text(
                    text = "$pendingKmClaims Monthly KM Claim${if (pendingKmClaims > 1) "s" else ""} Awaiting Approval",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColors.closingFg,
                  )
                  Text(
                    text = "Tap to review & approve supervisor travel claims",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColors.closingFg.copy(alpha = 0.85f),
                  )
                }
              }
              Text(
                text = "Review →",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = statusColors.closingFg,
              )
            }
          }
        }
      }

      // Quick Actions Rows
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = onCreateFarmerClick,
              modifier = Modifier
                .weight(1f)
                .testTag("overview_create_farmer_btn"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Create Farmer", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
              onClick = onCreateBatchClick,
              modifier = Modifier
                .weight(1f)
                .testTag("overview_create_batch_btn"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Create Batch", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onCreateZoneClick,
              modifier = Modifier
                .weight(1f)
                .testTag("overview_create_zone_btn"),
            ) {
              Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Create Zone", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
              onClick = onOpenZoneReport,
              modifier = Modifier
                .weight(1f)
                .testTag("overview_zone_pdf_btn"),
            ) {
              Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Zone Daily PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledTonalButton(
              onClick = onFeedDispatchClick,
              modifier = Modifier
                .weight(1f)
                .testTag("overview_feed_dispatch_btn"),
            ) {
              Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Feed Dispatch", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
              onClick = onBirdSaleClick,
              modifier = Modifier
                .weight(1f)
                .testTag("overview_bird_sale_btn"),
            ) {
              Icon(Icons.Rounded.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(6.dp))
              Text("Bird Sale", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }

      // Farm Status Matrix
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Farm & Shed Status",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          val cards = listOf(
            FarmStatusTileData(
              title = "Active Farm",
              count = activeFarms,
              subtitle = "Live flocks growing",
              icon = Icons.Rounded.Agriculture,
              bg = statusColors.activeBg,
              fg = statusColors.activeFg,
              filter = FarmFilter.ACTIVE,
              tag = "tile_active_farms",
            ),
            FarmStatusTileData(
              title = "Closing Farm",
              count = closingFarms,
              subtitle = "Lifting & settlement",
              icon = Icons.Rounded.HourglassTop,
              bg = statusColors.closingBg,
              fg = statusColors.closingFg,
              filter = FarmFilter.CLOSING,
              tag = "tile_closing_farms",
            ),
            FarmStatusTileData(
              title = "Empty Farm",
              count = emptyFarms,
              subtitle = "Ready for batch",
              icon = Icons.Rounded.CheckCircleOutline,
              bg = statusColors.emptyBg,
              fg = statusColors.emptyFg,
              filter = FarmFilter.EMPTY,
              tag = "tile_empty_farms",
            ),
            FarmStatusTileData(
              title = "Cancel Farm List",
              count = cancelledFarms,
              subtitle = "Paused / cancelled",
              icon = Icons.Rounded.Block,
              bg = statusColors.cancelledBg,
              fg = statusColors.cancelledFg,
              filter = FarmFilter.CANCELLED,
              tag = "tile_cancelled_farms",
            ),
          )

          if (isWide) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              cards.forEach { tile ->
                FarmStatusTile(
                  data = tile,
                  onClick = { onOpenFarmsFilter(tile.filter) },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                FarmStatusTile(cards[0], { onOpenFarmsFilter(cards[0].filter) }, Modifier.weight(1f))
                FarmStatusTile(cards[1], { onOpenFarmsFilter(cards[1].filter) }, Modifier.weight(1f))
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                FarmStatusTile(cards[2], { onOpenFarmsFilter(cards[2].filter) }, Modifier.weight(1f))
                FarmStatusTile(cards[3], { onOpenFarmsFilter(cards[3].filter) }, Modifier.weight(1f))
              }
            }
          }
        }
      }

      // Operational KPIs
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Production Telemetry",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          if (isWide) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              KpiMetricCard(
                title = "Live Birds",
                value = formatNumber(totalLiveBirds),
                subtitle = "${openBatches.size} active/closing batches",
                icon = Icons.Rounded.Pets,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "Average FCR",
                value = if (avgFcr > 0.0) formatDecimal(avgFcr) else "—",
                subtitle = "${formatDecimal(totalFeedKg / 1000.0, 1)} t feed used",
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "Feed Dispatched",
                value = "${formatNumber(totalDispatchedBags)} bags",
                subtitle = "${feedDispatches.size} challans issued",
                icon = Icons.Rounded.Inventory2,
                accentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "Settled Payouts",
                value = formatCurrency(totalSettled),
                subtitle = "${closedBatches.size} closed batches",
                icon = Icons.Rounded.CurrencyRupee,
                modifier = Modifier.weight(1f),
              )
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                  title = "Live Birds",
                  value = formatNumber(totalLiveBirds),
                  subtitle = "${openBatches.size} open batches",
                  icon = Icons.Rounded.Pets,
                  modifier = Modifier.weight(1f),
                )
                KpiMetricCard(
                  title = "Average FCR",
                  value = if (avgFcr > 0.0) formatDecimal(avgFcr) else "—",
                  subtitle = "${formatDecimal(totalFeedKg / 1000.0, 1)} t feed",
                  icon = Icons.AutoMirrored.Rounded.TrendingUp,
                  accentColor = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.weight(1f),
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                  title = "Feed Issued",
                  value = "${formatNumber(totalDispatchedBags)} bags",
                  subtitle = "${feedDispatches.size} dispatches",
                  icon = Icons.Rounded.Inventory2,
                  accentColor = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.weight(1f),
                )
                KpiMetricCard(
                  title = "Settled Total",
                  value = formatCurrency(totalSettled),
                  subtitle = "${closedBatches.size} closed",
                  icon = Icons.Rounded.CurrencyRupee,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      // Open Batches Snapshot
      item {
        Text(
          text = "Live & Closing Batches",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      if (openBatches.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Rounded.Warehouse,
            title = if (farmers.isEmpty()) "Start SPF Integration Network" else "No Active Batches Yet",
            description = if (farmers.isEmpty()) {
              "Add zones and contract farmer profiles, then place chicks to start tracking daily performance, sales, and settlements."
            } else {
              "You have $emptyFarms Empty Farm(s) ready for chick placement. Create a batch to begin tracking."
            },
            actionLabel = if (farmers.isEmpty()) "Create Farmer" else "Create Batch",
            onAction = if (farmers.isEmpty()) onCreateFarmerClick else onCreateBatchClick,
          )
        }
      } else {
        items(openBatches.size, key = { openBatches[it].id }) { index ->
          val batch = openBatches[index]
          BatchSnapshotRowCard(
            batch = batch,
            todayEpochDay = todayEpochDay,
            onClick = { onOpenBatchDetail(batch.id) },
          )
        }
      }
    }
  }
}

private data class FarmStatusTileData(
  val title: String,
  val count: Int,
  val subtitle: String,
  val icon: ImageVector,
  val bg: Color,
  val fg: Color,
  val filter: FarmFilter,
  val tag: String,
)

@Composable
private fun FarmStatusTile(
  data: FarmStatusTileData,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .testTag(data.tag),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = data.bg),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = data.icon,
          contentDescription = data.title,
          tint = data.fg,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = data.count.toString(),
          style = MonoMetricLarge,
          color = data.fg,
        )
      }
      Text(
        text = data.title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = data.fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = data.subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = data.fg.copy(alpha = 0.8f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun BatchSnapshotRowCard(
  batch: FlockBatch,
  todayEpochDay: Long,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = batch.batchCode,
            style = MaterialTheme.typography.titleMedium,
          )
          BatchStatusBadge(status = batch.statusEnum)
        }
        Text(
          text = "${batch.farmCode} • ${batch.farmerName} • Day ${batch.currentAgeDays(todayEpochDay)}/${batch.targetMarketAgeDays}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = if (batch.effectiveFcr > 0.0) "FCR ${formatDecimal(batch.effectiveFcr)}" else "${formatNumber(batch.survivingBirds)} birds",
          style = MonoMetricMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.MonitorWeight,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${batch.latestAvgWeightGrams.toInt()} g • ${formatDecimal(batch.mortalityPercent, 1)}% mort",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
