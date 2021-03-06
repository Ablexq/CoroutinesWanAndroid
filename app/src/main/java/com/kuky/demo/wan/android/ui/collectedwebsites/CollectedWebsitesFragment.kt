package com.kuky.demo.wan.android.ui.collectedwebsites

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.kuky.demo.wan.android.R
import com.kuky.demo.wan.android.base.*
import com.kuky.demo.wan.android.databinding.FragmentCollectedWebsitesBinding
import com.kuky.demo.wan.android.entity.WebsiteData
import com.kuky.demo.wan.android.ui.dialog.CollectedWebsiteDialogFragment
import com.kuky.demo.wan.android.ui.websitedetail.WebsiteDetailFragment
import com.kuky.demo.wan.android.ui.widget.ErrorReload
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast

/**
 * @author kuky.
 * @description
 */
class CollectedWebsitesFragment : BaseFragment<FragmentCollectedWebsitesBinding>() {

    private val mViewModel by lazy {
        ViewModelProvider(requireActivity(), CollectedWebsitesModelFactory(CollectedWebsitesRepository()))
            .get(CollectedWebsitesViewModel::class.java)
    }

    private val mAdapter by lazy { CollectedWebsitesAdapter() }

    private val editSelector by lazy { arrayListOf(resources.getString(R.string.del_website), resources.getString(R.string.edit_website)) }

    override fun actionsOnViewInflate() {
        fetchWebSitesData(false)
    }

    override fun getLayoutId(): Int = R.layout.fragment_collected_websites

    override fun initFragment(view: View, savedInstanceState: Bundle?) {
        mBinding?.let { binding ->
            binding.refreshColor = R.color.colorAccent
            binding.refreshListener = SwipeRefreshLayout.OnRefreshListener {
                fetchWebSitesData()
            }

            binding.fragment = this
            binding.adapter = mAdapter
            binding.listener = OnItemClickListener { position, _ ->
                mAdapter.getItemData(position)?.let {
                    WebsiteDetailFragment.viewDetail(
                        mNavController,
                        R.id.action_collectionFragment_to_websiteDetailFragment,
                        it.link
                    )
                }
            }
            binding.longListener = OnItemLongClickListener { position, _ ->
                mAdapter.getItemData(position)?.let { data ->
                    requireContext().selector(items = editSelector) { _, i ->
                        when (i) {
                            0 -> mViewModel.deleteWebsite(data.id, {
                                requireContext().toast("删除成功")
                                mAdapter.removeItem(position)
                            }, {
                                requireContext().toast(it)
                            })

                            1 -> {
                                CollectedWebsiteDialogFragment().apply {
                                    editMode = true
                                    injectWebsiteData(data)
                                }.show(childFragmentManager, "edit_website")
                            }
                        }
                    }
                }
            }

            binding.errorReload = ErrorReload { fetchWebSitesData() }

            binding.gesture = DoubleClickListener {
                singleTap = {
                    CollectedWebsiteDialogFragment().apply {
                        editMode = false
                        injectWebsiteData()
                    }.showAllowStateLoss(childFragmentManager, "new_website")
                }
            }
        }
    }

    fun scrollToTop() = mBinding?.websiteList?.scrollToTop()

    private fun fetchWebSitesData(isRefresh: Boolean = true) {
        mViewModel.fetchWebSitesData()
        mViewModel.netState.observe(this, Observer {
            when (it.state) {
                State.RUNNING -> injectStates(refreshing = true, loading = !isRefresh)

                State.SUCCESS -> injectStates()

                // 非 paging 加载情况直接设置 error status
                State.FAILED -> injectStates(error = true)
            }
        })

        mBinding?.errorStatus = false
        mBinding?.refreshing = true
        mViewModel.mWebsitesData.observe(this, Observer {
            mBinding?.emptyStatus = it.isNullOrEmpty()
            mAdapter.update(it as MutableList<WebsiteData>?)
        })
    }

    private fun injectStates(refreshing: Boolean = false, loading: Boolean = false, error: Boolean = false) {
        mBinding?.let { binding ->
            binding.refreshing = refreshing
            binding.loadingStatus = loading
            binding.errorStatus = error
        }
    }
}