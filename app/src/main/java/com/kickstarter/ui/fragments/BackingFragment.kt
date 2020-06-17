package com.kickstarter.ui.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding.view.RxView
import com.kickstarter.R
import com.kickstarter.extensions.showSnackbar
import com.kickstarter.libs.BaseFragment
import com.kickstarter.libs.Either
import com.kickstarter.libs.SwipeRefresher
import com.kickstarter.libs.qualifiers.RequiresFragmentViewModel
import com.kickstarter.libs.rx.transformers.Transformers
import com.kickstarter.libs.transformations.CircleTransformation
import com.kickstarter.libs.utils.ProjectUtils
import com.kickstarter.libs.utils.ViewUtils
import com.kickstarter.models.Reward
import com.kickstarter.ui.adapters.RewardAndAddOnsAdapter
import com.kickstarter.ui.data.PledgeStatusData
import com.kickstarter.ui.data.ProjectData
import com.kickstarter.viewmodels.BackingFragmentViewModel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_backing.*
import kotlinx.android.synthetic.main.fragment_backing_section_bonus.*
import kotlinx.android.synthetic.main.fragment_backing_section_delivery_date_reminder.*
import kotlinx.android.synthetic.main.fragment_backing_section_reward_delivery.*
import kotlinx.android.synthetic.main.fragment_backing_section_summary_total.*
import kotlinx.android.synthetic.main.fragment_pledge_section_summary_pledge.*
import kotlinx.android.synthetic.main.fragment_pledge_section_summary_shipping.*
import kotlinx.android.synthetic.main.reward_card_details.*


@RequiresFragmentViewModel(BackingFragmentViewModel.ViewModel::class)
class BackingFragment : BaseFragment<BackingFragmentViewModel.ViewModel>(), RewardAndAddOnsAdapter.ViewListener {

    private var rewardsAndAddOnsAdapter = RewardAndAddOnsAdapter(this)

    interface BackingDelegate {
        fun refreshProject()
        fun showFixPaymentMethod()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_backing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        this.viewModel.outputs.backerAvatar()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { setBackerImageView(it) }

        this.viewModel.outputs.backerName()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { backer_name.text = it }

        this.viewModel.outputs.backerNumber()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { setBackerNumberText(it) }

        this.viewModel.outputs.cardLogo()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { reward_card_logo.setImageResource(it) }

        this.viewModel.outputs.cardExpiration()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { setCardExpirationText(it) }

        this.viewModel.outputs.cardIssuer()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { setCardIssuerContentDescription(it) }

        this.viewModel.outputs.cardLastFour()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { setCardLastFourText(it) }

        this.viewModel.outputs.fixPaymentMethodButtonIsGone()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { ViewUtils.setGone(fix_payment_method_button, it) }

        this.viewModel.outputs.fixPaymentMethodButtonIsGone()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { ViewUtils.setGone(fix_payment_method_message, it) }

        this.viewModel.outputs.notifyDelegateToRefreshProject()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { (activity as BackingDelegate?)?.refreshProject() }

        this.viewModel.outputs.notifyDelegateToShowFixPledge()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { (activity as BackingDelegate?)?.showFixPaymentMethod() }

        this.viewModel.outputs.paymentMethodIsGone()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { ViewUtils.setGone(payment_method, it) }

        this.viewModel.outputs.pledgeAmount()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { pledge_summary_amount.text = it }

        this.viewModel.outputs.pledgeDate()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { setPledgeDateText(it) }

        this.viewModel.outputs.pledgeStatusData()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { setPledgeStatusText(it) }

        this.viewModel.outputs.pledgeSummaryIsGone()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { ViewUtils.setGone(pledge_summary, it) }

        this.viewModel.outputs.projectDataAndReward()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe {
                    bindDataToRewardViewHolder(it)
                }

        this.viewModel.outputs.receivedCheckboxChecked()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { estimated_delivery_checkbox.isChecked = it }

        this.viewModel.outputs.receivedSectionIsGone()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe {
                    ViewUtils.setGone(received_section, it)
                    ViewUtils.setGone(estimated_delivery_label_2, !it)
                }

        this.viewModel.outputs.shippingAmount()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { shipping_summary_amount.text = it }

        this.viewModel.outputs.shippingLocation()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { shipping_label.text = String.format("%s: %s", getString(R.string.Shipping), it) }

        this.viewModel.outputs.shippingSummaryIsGone()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { ViewUtils.setGone(shipping_summary, it) }

        this.viewModel.outputs.showUpdatePledgeSuccess()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { showSnackbar(view, getString(R.string.Got_it_your_changes_have_been_saved)) }

        this.viewModel.outputs.totalAmount()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { total_summary_amount.text = it }

        this.viewModel.outputs.bonusSupport()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { bonus_summary_amount.text = it.toString() }

        this.viewModel.outputs.estimatedDelivery()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe {

                    estimated_delivery_label.text = viewModel.environment.ksString().format(getString(R.string.pledge_delivery_delivered), "estimated_delivery", it)

                    val stringToBold = getString(R.string.rewards_info_estimated_delivery)
                    setBoldSpanOnTextView(stringToBold.length, estimated_delivery_label)

                    estimated_delivery_label_2.text = "${estimated_delivery_label_2.text} $it"
                    setBoldSpanOnTextView(stringToBold.length, estimated_delivery_label_2)

                }

        this.viewModel.environment.currentUser().observable()
                .compose(bindToLifecycle())
                .compose(Transformers.observeForUI())
                .subscribe { user ->
                    this.viewModel.outputs.projectDataAndReward().map { it.first }
                            .compose(bindToLifecycle())
                            .compose(Transformers.observeForUI())
                            .subscribe { projectData ->

                                if (ProjectUtils.userIsCreator(projectData.project(), user)) {
                                    pledge_details_label.text = getString(R.string.pledge_details_creator)
                                    ViewUtils.setGone(received_section, true)
                                    ViewUtils.setGone(delivery_disclaimer_section, true)
                                    ViewUtils.setGone(estimated_delivery_label_2, false)
                                    estimated_delivery_label_2.setPadding(0, 0, 0, resources.getDimension(R.dimen.grid_3).toInt())

                                } else {
                                    pledge_details_label.text = getString(R.string.pledge_details_backer)
                                }

                            }
                }


        val boldPortionLength = delivery_reminder_label.text.toString().split(".").first().length
        setBoldSpanOnTextView(boldPortionLength, delivery_reminder_label)

        SwipeRefresher(
                this, backing_swipe_refresh_layout, { this.viewModel.inputs.refreshProject() }, { this.viewModel.outputs.swipeRefresherProgressIsVisible() }
        )

        RxView.clicks(fix_payment_method_button)
                .compose(bindToLifecycle())
                .subscribe { this.viewModel.inputs.fixPaymentMethodButtonClicked() }

        RxView.clicks(estimated_delivery_checkbox)
                .compose(bindToLifecycle())
                .subscribe { this.viewModel.inputs.receivedCheckboxToggled(estimated_delivery_checkbox.isChecked) }
    }

    private fun setBoldSpanOnTextView(numCharacters: Int, textView: TextView) {
        val spannable = SpannableString(textView.text)
        spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0, numCharacters,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannable
    }

    fun configureWith(projectData: ProjectData) {
        this.viewModel.inputs.configureWith(projectData)
    }

    fun pledgeSuccessfullyUpdated() {
        this.viewModel.inputs.pledgeSuccessfullyUpdated()
    }

    private fun bindDataToRewardViewHolder(projectAndReward: Pair<ProjectData, Reward>) {
        val project = projectAndReward.first
        val reward = projectAndReward.second

        val projectAndRw = Pair(project, reward)
        rewardsAndAddOnsAdapter.populateDataForRewad(projectAndRw)
    }

    private fun setBackerImageView(url: String) {
        context?.apply {
            Picasso.with(this).load(url)
                    .transform(CircleTransformation())
                    .into(backing_avatar)
        }
    }

    private fun setBackerNumberText(it: String?) {
        backer_number.text = this.viewModel.ksString.format(getString(R.string.backer_modal_backer_number), "backer_number", it)
    }

    private fun setCardExpirationText(expiration: String) {
        reward_card_expiration_date.text = this.viewModel.ksString.format(getString(R.string.Credit_card_expiration),
                "expiration_date", expiration)
    }

    private fun setCardIssuerContentDescription(cardIssuerOrStringRes: Either<String, Int>) {
        val cardIssuer = cardIssuerOrStringRes.left()
        val stringRes = cardIssuerOrStringRes.right()
        reward_card_logo.contentDescription = stringRes?.let { getString(it) } ?: cardIssuer
    }

    private fun setCardLastFourText(lastFour: String) {
        reward_card_last_four.text = this.viewModel.ksString.format(getString(R.string.payment_method_last_four),
                "last_four",
                lastFour)
    }

    private fun setPledgeDateText(pledgeDate: String) {
        backing_date.text = this.viewModel.ksString.format(getString(R.string.As_of_pledge_date), "pledge_date", pledgeDate)
    }

    private fun setPledgeStatusText(pledgeStatusData: PledgeStatusData) {
        val ksString = this.viewModel.ksString
        val pledgeStatusText = pledgeStatusData.statusStringRes?.let {
            when (pledgeStatusData.statusStringRes) {
                R.string.If_the_project_reaches_its_funding_goal_you_will_be_charged_total_on_project_deadline -> {
                    ksString.format(getString(it),
                            "total", pledgeStatusData.pledgeTotal,
                            "project_deadline", pledgeStatusData.projectDeadline)
                }
                else -> getString(it)
            }
        }

        pledgeStatusText?.let { statusText ->
            val spannablePledgeStatus = SpannableString(statusText)
            pledgeStatusData.pledgeTotal?.let { ViewUtils.addBoldSpan(spannablePledgeStatus, it) }
            pledgeStatusData.projectDeadline?.let { ViewUtils.addBoldSpan(spannablePledgeStatus, it) }

            backer_pledge_status.text = spannablePledgeStatus
        } ?: run {
            backer_pledge_status.text = null
        }
    }

    private fun setupRecyclerView() {
        reward_add_on_recycler.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        reward_add_on_recycler.adapter = rewardsAndAddOnsAdapter
    }

    override fun rewardClicked(reward: Reward) {
        // TODO in https://kickstarter.atlassian.net/browse/NT-1290
    }
}
