package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.ArrayDeque

@RunWith(AndroidJUnit4::class)
class CustomerCenterNavigationStateTest {

    @Test
    fun `test initial state has Main destination and cannot navigate back`() {
        val navigationState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test Title")

        assertThat(navigationState.currentDestination).isInstanceOf(CustomerCenterDestination.Main::class.java)
        assertThat(navigationState.canNavigateBack).isFalse()
        assertThat(navigationState.backStack.size).isEqualTo(1)
    }

    @Test
    fun `test initial state with null title`() {
        val navigationState = CustomerCenterNavigationState(showingActivePurchasesScreen = false, managementScreenTitle = null)

        val mainDestination = navigationState.currentDestination as CustomerCenterDestination.Main
        assertThat(mainDestination.title).isNull()
    }

    @Test
    fun `test push adds destination to stack and enables back navigation`() {
        val initialState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")

        assertThat(initialState.canNavigateBack).isFalse()
        assertThat(initialState.backStack.size).isEqualTo(1)

        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback"
        )

        val newState = initialState.push(feedbackDestination)

        assertThat(newState.currentDestination).isEqualTo(feedbackDestination)
        assertThat(newState.canNavigateBack).isTrue()
        assertThat(newState.backStack.size).isEqualTo(2)
    }

    @Test
    fun `test push multiple destinations creates correct stack order`() {
        val initialState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")
        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback"
        )
        val promotionalOffer = createMockPromotionalOffer()
        val promoDestination = CustomerCenterDestination.PromotionalOffer(
            data = promotionalOffer
        )

        val stateAfterPushing = initialState.push(feedbackDestination).push(promoDestination)

        assertThat(stateAfterPushing.currentDestination).isEqualTo(promoDestination)
        assertThat(stateAfterPushing.canNavigateBack).isTrue()
        assertThat(stateAfterPushing.backStack.size).isEqualTo(3)

        // toList puts most recent first
        val stackList = stateAfterPushing.backStack.toList()
        assertThat(stackList[0]).isEqualTo(promoDestination)
        assertThat(stackList[1]).isEqualTo(feedbackDestination)
        assertThat(stackList[2]).isInstanceOf(CustomerCenterDestination.Main::class.java)
    }

    @Test
    fun `test pop removes current destination and goes back to previous`() {
        val initialState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")
        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback"
        )

        val pushedState = initialState.push(feedbackDestination)
        val poppedState = pushedState.pop()

        assertThat(poppedState.currentDestination).isInstanceOf(CustomerCenterDestination.Main::class.java)
        assertThat(poppedState.canNavigateBack).isFalse()
        assertThat(poppedState.backStack.size).isEqualTo(1)
    }

    @Test
    fun `test pop when cannot navigate back returns same state`() {
        val initialState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")

        val poppedState = initialState.pop()

        assertThat(poppedState).isEqualTo(initialState)
        assertThat(poppedState.currentDestination).isInstanceOf(CustomerCenterDestination.Main::class.java)
        assertThat(poppedState.canNavigateBack).isFalse()
    }

    @Test
    fun `test popToMain from nested navigation returns to main`() {
        val initialState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")
        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback"
        )
        val promotionalOffer = createMockPromotionalOffer()
        val promoDestination = CustomerCenterDestination.PromotionalOffer(
            data = promotionalOffer
        )

        val nestedState = initialState
            .push(feedbackDestination)
            .push(promoDestination)
        val mainState = nestedState.popToMain()

        assertThat(mainState.currentDestination).isInstanceOf(CustomerCenterDestination.Main::class.java)
        assertThat(mainState.canNavigateBack).isFalse()
        assertThat(mainState.backStack.size).isEqualTo(1)
    }

    @Test
    fun `test popToMain when already at main returns equivalent state`() {
        val initialState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")

        val mainState = initialState.popToMain()

        assertThat(mainState.currentDestination).isInstanceOf(CustomerCenterDestination.Main::class.java)
        assertThat(mainState.canNavigateBack).isFalse()
        assertThat(mainState.backStack.size).isEqualTo(1)
    }

    @Test
    fun `test popToMain with empty stack logs error and returns unchanged state`() {
        val emptyStackState = CustomerCenterNavigationState(
            showingActivePurchasesScreen = true,
            managementScreenTitle = "Test",
            backStack = ArrayDeque()
        )

        val result = emptyStackState.popToMain()

        assertThat(result).isEqualTo(emptyStackState)
    }

    @Test
    fun `test isBackwardTransition returns true when going from any screen to Main`() {
        val navigationState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")
        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback"
        )
        val mainDestination = CustomerCenterDestination.Main(showingActivePurchasesScreen = true, managementScreenTitle = "Main")
        val promotionalOffer = createMockPromotionalOffer()
        val promoDestination = CustomerCenterDestination.PromotionalOffer(
            data = promotionalOffer
        )

        val stateWithMultipleScreens = navigationState
            .push(feedbackDestination)
            .push(promoDestination)

        assertThat(
            stateWithMultipleScreens.isBackwardTransition(feedbackDestination, mainDestination)
        ).isTrue()
        assertThat(
            stateWithMultipleScreens.isBackwardTransition(promoDestination, mainDestination)
        ).isTrue()
    }

    @Test
    fun `test isBackwardTransition returns false when going from Main to any screen`() {
        val navigationState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")
        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback"
        )
        val mainDestination = CustomerCenterDestination.Main(showingActivePurchasesScreen = true, managementScreenTitle = "Main")

        val stateWithFeedback = navigationState.push(feedbackDestination)

        assertThat(
            stateWithFeedback.isBackwardTransition(mainDestination, feedbackDestination)
        ).isFalse()
    }

    @Test
    fun `test isBackwardTransition uses stack positions for non-Main transitions`() {
        val navigationState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")
        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback"
        )
        val promotionalOffer = createMockPromotionalOffer()
        val promoDestination = CustomerCenterDestination.PromotionalOffer(
            data = promotionalOffer
        )

        val stateWithBothScreens = navigationState
            .push(feedbackDestination)
            .push(promoDestination)

        // Going from promo (index 0) to feedback (index 1) is backward
        assertThat(
            stateWithBothScreens.isBackwardTransition(promoDestination, feedbackDestination)
        ).isTrue()

        // Going from feedback (index 1) to promo (index 0) is forward
        assertThat(
            stateWithBothScreens.isBackwardTransition(feedbackDestination, promoDestination)
        ).isFalse()
    }

    @Test
    fun `test isBackwardTransition returns false when destinations not in stack`() {
        val navigationState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")
        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback"
        )
        val promotionalOffer = createMockPromotionalOffer()
        val promoDestination = CustomerCenterDestination.PromotionalOffer(
            data = promotionalOffer
        )

        // Only add feedback to stack, promo is not in stack
        val stateWithOnlyFeedback = navigationState.push(feedbackDestination)

        assertThat(
            stateWithOnlyFeedback.isBackwardTransition(feedbackDestination, promoDestination)
        ).isFalse()
        assertThat(
            stateWithOnlyFeedback.isBackwardTransition(promoDestination, feedbackDestination)
        ).isFalse()
    }

    @Test
    fun `test currentDestination returns Main when stack is empty`() {
        val emptyStackState = CustomerCenterNavigationState(
            showingActivePurchasesScreen = true,
            managementScreenTitle = "Test",
            backStack = ArrayDeque()
        )

        val currentDestination = emptyStackState.currentDestination

        assertThat(currentDestination).isInstanceOf(CustomerCenterDestination.Main::class.java)
        assertThat((currentDestination as CustomerCenterDestination.Main).title).isEqualTo("Test")
    }

    @Test
    fun `test canNavigateBack is false when stack has only one item`() {
        val navigationState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")

        assertThat(navigationState.canNavigateBack).isFalse()
        assertThat(navigationState.backStack.size).isEqualTo(1)
    }

    @Test
    fun `test canNavigateBack is false when stack is empty`() {
        val emptyStackState = CustomerCenterNavigationState(
            showingActivePurchasesScreen = true,
            managementScreenTitle = "Test",
            backStack = ArrayDeque()
        )

        assertThat(emptyStackState.canNavigateBack).isFalse()
    }

    @Test
    fun `test complex navigation scenarios with multiple back operations and main screens`() {
        val navigationState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Root")
        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback"
        )
        val promotionalOffer = createMockPromotionalOffer()
        val promoDestination = CustomerCenterDestination.PromotionalOffer(
            data = promotionalOffer
        )
        val anotherMain = CustomerCenterDestination.Main(showingActivePurchasesScreen = true, managementScreenTitle = "Another Main")

        val complexState = navigationState
            .push(feedbackDestination)
            .push(anotherMain)
            .push(promoDestination)

        assertThat(complexState.backStack.size).isEqualTo(4)
        assertThat(complexState.canNavigateBack).isTrue()
        assertThat(complexState.currentDestination).isEqualTo(promoDestination)

        val afterOnePop = complexState.pop()
        assertThat(afterOnePop.currentDestination).isEqualTo(anotherMain)
        assertThat(afterOnePop.canNavigateBack).isTrue()

        val backToMain = complexState.popToMain()
        assertThat(backToMain.currentDestination).isEqualTo(anotherMain)
        assertThat(backToMain.canNavigateBack).isTrue() // Still has the original main below it
    }

    @Test
    fun `test destination equality in stack operations`() {
        val navigationState = CustomerCenterNavigationState(showingActivePurchasesScreen = true, managementScreenTitle = "Test")
        val feedbackSurvey = createMockFeedbackSurvey()
        val feedbackDestination1 = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback 1"
        )
        val feedbackDestination2 = CustomerCenterDestination.FeedbackSurvey(
            data = FeedbackSurveyData(
                feedbackSurvey = feedbackSurvey,
                onAnswerSubmitted = {}
            ),
            title = "Feedback 2"
        )

        val stateWithFeedback1 = navigationState.push(feedbackDestination1)
        val stateWithBoth = stateWithFeedback1.push(feedbackDestination2)

        // Different destinations should not be equal
        assertThat(feedbackDestination1).isNotEqualTo(feedbackDestination2)
        
        // isBackwardTransition should work correctly with different instances
        assertThat(
            stateWithBoth.isBackwardTransition(feedbackDestination2, feedbackDestination1)
        ).isTrue()
    }

    private fun createMockFeedbackSurvey(): CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey {
        return CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey(
            title = "Test Survey",
            options = listOf(
                CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option(
                    id = "option1",
                    title = "Option 1",
                    promotionalOffer = null
                )
            )
        )
    }

    private fun createMockPromotionalOffer(): PromotionalOfferData {
        return PromotionalOfferData(
            configuredPromotionalOffer = mockk(),
            subscriptionOption = mockk<SubscriptionOption>(),
            originalPath = mockk(),
            localizedPricingPhasesDescription = "Test Description"
        )
    }
}
